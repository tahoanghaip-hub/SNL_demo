package com.local.snldemo.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.local.snldemo.data.local.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

object StorageHelper {

    /**
     * Computes a deterministic SHA-256 hex string from notification properties.
     * Used for duplicate deduplication and conflict-free merge imports.
     */
    fun computeUniqueHash(
        packageName: String,
        senderName: String,
        content: String,
        timestamp: Long
    ): String {
        val input = "$packageName|$senderName|$content|$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format(Locale.US, "%02x", b))
        }
        return sb.toString()
    }

    /**
     * Resizes the app icon drawable to 64x64px and saves as WebP (80% quality)
     * in context.filesDir/app_icons/<packageName>.webp.
     * Returns the absolute path on success, or null on failure.
     */
    fun saveAppIconWebP(context: Context, packageName: String, drawable: Drawable): String? {
        return try {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, 64, 64)
            drawable.draw(canvas)

            val dir = File(context.filesDir, "app_icons")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "$packageName.webp")
            FileOutputStream(file).use { out ->
                @Suppress("DEPRECATION")
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(format, 80, out)
            }
            bitmap.recycle()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resizes sender avatar to 64x64px and saves as WebP in context.filesDir/avatars/<packageName>/<keyHash>.webp.
     */
    fun saveSenderAvatarWebP(
        context: Context,
        packageName: String,
        keyHash: String,
        bitmap: Bitmap
    ): String? {
        return try {
            val scaled = if (bitmap.width != 64 || bitmap.height != 64) {
                Bitmap.createScaledBitmap(bitmap, 64, 64, true)
            } else {
                bitmap
            }
            val dir = File(context.filesDir, "avatars/$packageName")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "$keyHash.webp")
            FileOutputStream(file).use { out ->
                @Suppress("DEPRECATION")
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
                scaled.compress(format, 80, out)
            }
            if (scaled != bitmap) {
                scaled.recycle()
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a sender avatar Drawable to 64x64px Bitmap and delegates to saveSenderAvatarWebP.
     */
    fun saveSenderAvatarWebP(
        context: Context,
        packageName: String,
        keyHash: String,
        drawable: Drawable
    ): String? {
        return try {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, 64, 64)
            drawable.draw(canvas)
            val path = saveSenderAvatarWebP(context, packageName, keyHash, bitmap)
            bitmap.recycle()
            path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Scales down notification image if width > 1080px (maintaining aspect ratio),
     * compresses to WebP 80% quality, and saves in context.filesDir/notif_images/<packageName>/img_<timestamp>.webp.
     */
    fun saveNotificationImageWebP(
        context: Context,
        packageName: String,
        bitmap: Bitmap
    ): String? {
        return try {
            val targetBitmap = if (bitmap.width > 1080) {
                val targetWidth = 1080
                val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            } else {
                bitmap
            }

            val dir = File(context.filesDir, "notif_images/$packageName")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.webp"
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                @Suppress("DEPRECATION")
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
                targetBitmap.compress(format, 80, out)
            }
            if (targetBitmap != bitmap) {
                targetBitmap.recycle()
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies an internal private image to the public Pictures/SNL_Saved gallery folder
     * using MediaStore for 1-click user saving without requiring dangerous permissions.
     */
    fun copyImageToPublicGallery(context: Context, imagePath: String): Boolean {
        return try {
            val srcFile = File(imagePath)
            if (!srcFile.exists()) return false

            val fileName = "SNL_${System.currentTimeMillis()}.webp"
            val mimeType = "image/webp"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SNL_Saved")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val collection =
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = context.contentResolver.insert(collection, values) ?: return false

                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(srcFile).use { input ->
                        input.copyTo(out)
                    }
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                true
            } else {
                val picturesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val targetDir = File(picturesDir, "SNL_Saved")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val destFile = File(targetDir, fileName)
                srcFile.copyTo(destFile, overwrite = true)

                @Suppress("DEPRECATION")
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, destFile.absolutePath)
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Safely deletes a list of file paths.
     */
    fun deleteFiles(paths: List<String?>) {
        for (path in paths) {
            if (!path.isNullOrBlank()) {
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Recursively deletes all saved notification images and avatars for a specific package.
     */
    fun deleteAppImageFolder(context: Context, packageName: String) {
        try {
            val notifDir = File(context.filesDir, "notif_images/$packageName")
            if (notifDir.exists()) {
                notifDir.deleteRecursively()
            }
            val avatarDir = File(context.filesDir, "avatars/$packageName")
            if (avatarDir.exists()) {
                avatarDir.deleteRecursively()
            }
            val iconFile = File(context.filesDir, "app_icons/$packageName.webp")
            if (iconFile.exists()) {
                iconFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Calculates the total media size in bytes for a specific package (images + avatars).
     */
    fun getAppMediaBytes(context: Context, packageName: String): Long {
        var totalBytes = 0L
        val notifDir = File(context.filesDir, "notif_images/$packageName")
        totalBytes += getFolderSize(notifDir)
        val avatarDir = File(context.filesDir, "avatars/$packageName")
        totalBytes += getFolderSize(avatarDir)
        return totalBytes
    }

    /**
     * Calculates the total size of the database and files directory in bytes.
     */
    fun getDatabaseTotalBytes(context: Context): Long {
        var total = 0L
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        if (dbFile.exists()) {
            total += dbFile.length()
        }
        val walFile = File(dbFile.parentFile, "${dbFile.name}-wal")
        if (walFile.exists()) {
            total += walFile.length()
        }
        val shmFile = File(dbFile.parentFile, "${dbFile.name}-shm")
        if (shmFile.exists()) {
            total += shmFile.length()
        }

        val filesDir = context.filesDir
        total += getFolderSize(filesDir)
        return total
    }

    /**
     * Returns totalBytes, usedBytes, freeBytes of the internal data storage using StatFs.
     */
    fun getDeviceStorageInfo(): Triple<Long, Long, Long> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            Triple(totalBytes, usedBytes, freeBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            Triple(0L, 0L, 0L)
        }
    }

    /**
     * Formats bytes into human-readable string: B, KB, MB, GB, TB.
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val formatted = bytes / Math.pow(1024.0, index.toDouble())
        return if (index == 0) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.1f %s", formatted, units[index])
        }
    }

    private fun getFolderSize(fileOrDir: File?): Long {
        if (fileOrDir == null || !fileOrDir.exists()) return 0L
        if (fileOrDir.isFile) return fileOrDir.length()
        var size = 0L
        val children = fileOrDir.listFiles() ?: return 0L
        for (child in children) {
            size += getFolderSize(child)
        }
        return size
    }
}
