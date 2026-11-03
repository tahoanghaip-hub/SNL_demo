package com.local.snldemo.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.local.snldemo.data.local.AppDatabase
import com.local.snldemo.data.local.AppMetadataEntity
import com.local.snldemo.data.local.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupRestoreHelper {

    data class BackupMediaFile(
        val relativePath: String,
        val base64Data: String
    )

    data class FullBackupPayload(
        val version: Int = 1,
        val exportedAt: Long = System.currentTimeMillis(),
        val appMetadata: List<AppMetadataEntity>,
        val notifications: List<NotificationEntity>,
        val mediaFiles: List<BackupMediaFile>
    )

    data class RestoreResult(
        val success: Boolean,
        val totalInFile: Int,
        val newlyMergedCount: Int,
        val skippedDuplicateCount: Int,
        val errorMessage: String? = null
    )

    /**
     * Exports entire database and all associated local media (icons, avatars, images)
     * into a single, portable, self-contained JSON file.
     */
    suspend fun exportFullBackupToUri(context: Context, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val dao = AppDatabase.getInstance(context).notificationDao()
            val allMeta = dao.getAllAppMetadataSync()
            val allNotifs = dao.getAllNotificationsSync()
            val filesDir = context.filesDir

            val mediaFilesMap = mutableMapOf<String, BackupMediaFile>()
            val allPaths = mutableSetOf<String>()

            allMeta.forEach { m -> m.appIconPath?.let { allPaths.add(it) } }
            allNotifs.forEach { n ->
                n.senderAvatarPath?.let { allPaths.add(it) }
                n.imageAttachmentPath?.let { allPaths.add(it) }
            }

            for (absPath in allPaths) {
                try {
                    val file = File(absPath)
                    if (file.exists() && file.isFile && file.length() > 0) {
                        val rel = toRelativePath(filesDir, absPath) ?: continue
                        if (!mediaFilesMap.containsKey(rel)) {
                            val bytes = file.readBytes()
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            mediaFilesMap[rel] = BackupMediaFile(rel, base64)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Normalize internal paths to relative paths inside the backup payload
            val exportedMeta = allMeta.map { m ->
                m.copy(appIconPath = toRelativePath(filesDir, m.appIconPath))
            }
            val exportedNotifs = allNotifs.map { n ->
                n.copy(
                    senderAvatarPath = toRelativePath(filesDir, n.senderAvatarPath),
                    imageAttachmentPath = toRelativePath(filesDir, n.imageAttachmentPath)
                )
            }

            val payload = FullBackupPayload(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                appMetadata = exportedMeta,
                notifications = exportedNotifs,
                mediaFiles = mediaFilesMap.values.toList()
            )

            val json = Gson().toJson(payload)
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return@withContext false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Imports and merges a backup JSON file without duplicates, preserving timestamps and media.
     */
    suspend fun importAndMergeBackupFromUri(context: Context, sourceUri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val json = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext RestoreResult(false, 0, 0, 0, "Could not open source file")

            val payload = Gson().fromJson(json, FullBackupPayload::class.java)
                ?: return@withContext RestoreResult(false, 0, 0, 0, "Invalid backup format")

            val filesDir = context.filesDir
            val dao = AppDatabase.getInstance(context).notificationDao()

            // Step A: Restore Media Files
            for (media in payload.mediaFiles) {
                try {
                    val destFile = File(filesDir, media.relativePath)
                    if (!destFile.exists()) {
                        destFile.parentFile?.mkdirs()
                        val bytes = Base64.decode(media.base64Data, Base64.DEFAULT)
                        destFile.writeBytes(bytes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Step B: Merge App Metadata
            for (meta in payload.appMetadata) {
                val resolvedIconPath = meta.appIconPath?.let { rel ->
                    File(filesDir, rel).absolutePath
                }
                val existing = dao.getAppMetadataSync(meta.packageName)
                if (existing == null) {
                    dao.upsertAppMetadata(
                        meta.copy(appIconPath = resolvedIconPath)
                    )
                } else {
                    if (existing.appIconPath.isNullOrBlank() && resolvedIconPath != null) {
                        dao.upsertAppMetadata(existing.copy(appIconPath = resolvedIconPath))
                    }
                }
            }

            // Step C: Conflict-Free Notification Batch Merge
            val preparedList = payload.notifications.map { n ->
                val resolvedAvatar = n.senderAvatarPath?.let { rel ->
                    File(filesDir, rel).absolutePath
                }
                val resolvedImage = n.imageAttachmentPath?.let { rel ->
                    File(filesDir, rel).absolutePath
                }
                val hash = if (n.uniqueHash.isNotBlank()) {
                    n.uniqueHash
                } else {
                    StorageHelper.computeUniqueHash(n.packageName, n.senderName, n.content, n.timestamp)
                }
                n.copy(
                    id = 0, // Force auto-generation of ID to avoid primary key collisions
                    uniqueHash = hash,
                    senderAvatarPath = resolvedAvatar,
                    imageAttachmentPath = resolvedImage
                )
            }

            val rowIds = dao.insertNotificationsBatch(preparedList)
            val newlyMerged = rowIds.count { it != -1L }
            val skippedDuplicates = rowIds.count { it == -1L }

            RestoreResult(
                success = true,
                totalInFile = payload.notifications.size,
                newlyMergedCount = newlyMerged,
                skippedDuplicateCount = skippedDuplicates
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult(false, 0, 0, 0, e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Exports human-readable CSV with UTF-8 BOM so spreadsheet viewers properly display accents.
     */
    suspend fun exportCsvToUri(context: Context, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val dao = AppDatabase.getInstance(context).notificationDao()
            val allNotifs = dao.getAllNotificationsSync()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                // UTF-8 BOM for Excel
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                val writer = out.bufferedWriter(Charsets.UTF_8)
                writer.write("ID,Time,PackageName,AppName,Sender,Title,Content,IsRemoved,IsStarred,IsVideo\n")
                for (n in allNotifs) {
                    val timeStr = sdf.format(Date(n.timestamp))
                    val line = "${n.id},${escapeCsv(timeStr)},${escapeCsv(n.packageName)},${escapeCsv(n.appName)},${escapeCsv(n.senderName)},${escapeCsv(n.title)},${escapeCsv(n.content)},${n.isRemoved},${n.isStarred},${n.isVideoMessage}\n"
                    writer.write(line)
                }
                writer.flush()
            } ?: return@withContext false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun toRelativePath(filesDir: File, path: String?): String? {
        if (path.isNullOrBlank()) return null
        val norm = path.replace('\\', '/')
        val base = filesDir.absolutePath.replace('\\', '/')
        return if (norm.startsWith(base)) {
            norm.removePrefix(base).removePrefix("/")
        } else {
            when {
                norm.contains("app_icons/") -> "app_icons/" + norm.substringAfter("app_icons/")
                norm.contains("avatars/") -> "avatars/" + norm.substringAfter("avatars/")
                norm.contains("notif_images/") -> "notif_images/" + norm.substringAfter("notif_images/")
                else -> norm.substringAfterLast('/')
            }
        }
    }

    private fun escapeCsv(value: String): String {
        return "\"" + value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "") + "\""
    }
}

