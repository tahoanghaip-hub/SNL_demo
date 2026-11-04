package com.local.snldemo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.local.snldemo.MainActivity
import com.local.snldemo.R
import com.local.snldemo.data.local.AppDatabase
import com.local.snldemo.data.local.AppMetadataEntity
import com.local.snldemo.data.local.NotificationEntity
import com.local.snldemo.ui.i18n.AppLanguage
import com.local.snldemo.util.PreferenceManager
import com.local.snldemo.util.StorageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class NotifLoggerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory tracker for active unread notification line stacks per "$pkg|$senderName"
    private val activeUnreadStacks = ConcurrentHashMap<String, List<String>>()

    private val summaryRegex = Regex(
        """^(Bạn có \d+ tin nhắn mới|\d+ tin nhắn mới|\d+ new messages)""",
        RegexOption.IGNORE_CASE
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startSilentForeground()
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        startSilentForeground()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        val prefs = PreferenceManager.getInstance(this)
        if (prefs.isBackgroundEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requestRebind(ComponentName(this, NotifLoggerService::class.java))
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName
        if (pkg == packageName) return // Ignore self-notifications

        val prefs = PreferenceManager.getInstance(this)
        if (!prefs.isBackgroundEnabled) return

        val notif = sbn.notification ?: return

        // Filter out Ongoing & System Noise
        if (sbn.isOngoing) return
        if ((notif.flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        if ((notif.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) return
        if ((notif.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return // Skip group summary headers

        val category = notif.category
        if (category == Notification.CATEGORY_PROGRESS ||
            category == Notification.CATEGORY_TRANSPORT ||
            category == Notification.CATEGORY_SERVICE ||
            category == Notification.CATEGORY_SYSTEM
        ) {
            return
        }

        // Deep Content & Delta Extraction on Dispatchers.IO (Zero UI/Main thread overhead)
        serviceScope.launch {
            try {
                processNotificationPosted(sbn, notif, pkg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        if (sbn == null) return
        val pkg = sbn.packageName

        // CRITICAL BUGFIX: NEVER mark notifications as removed in Room here!
        // When user opens Zalo/Messenger to read messages, Android calls cancel() -> REASON_APP_CANCEL.
        // Instead, simply reset the in-memory unread stack for this package so future unread sessions start fresh.
        val prefix = "$pkg|"
        activeUnreadStacks.keys.filter { it.startsWith(prefix) }.forEach {
            activeUnreadStacks.remove(it)
        }
    }

    private suspend fun processNotificationPosted(
        sbn: StatusBarNotification,
        notif: Notification,
        pkg: String
    ) {
        val dao = AppDatabase.getInstance(this).notificationDao()

        // 1. Check or seed AppMetadata (Whitelist / Blacklist check)
        var meta = dao.getAppMetadataSync(pkg)
        if (meta == null) {
            val appInfo = try {
                packageManager.getApplicationInfo(pkg, 0)
            } catch (e: Exception) {
                null
            }
            val appName = try {
                appInfo?.loadLabel(packageManager)?.toString()?.ifBlank { pkg } ?: pkg
            } catch (e: Exception) {
                pkg
            }
            val iconDrawable = try {
                appInfo?.loadIcon(packageManager)
            } catch (e: Exception) {
                null
            }
            val iconPath = iconDrawable?.let {
                StorageHelper.saveAppIconWebP(this, pkg, it)
            }

            val newMeta = AppMetadataEntity(
                packageName = pkg,
                appName = appName,
                appIconPath = iconPath,
                isTrackingEnabled = true,
                isSaveImageEnabled = false
            )
            dao.upsertAppMetadata(newMeta)
            meta = newMeta
        }

        if (!meta.isTrackingEnabled) {
            return // App is disabled by user
        }

        // 2. Extract Titles & Sender Name
        val extras = notif.extras ?: Bundle.EMPTY
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim()
            ?: ""

        val convTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
            ?: ""

        val senderName = if (convTitle.isNotEmpty() && convTitle != rawTitle) {
            if (rawTitle.isNotEmpty()) "$convTitle • $rawTitle" else convTitle
        } else {
            rawTitle.ifEmpty { meta.appName }
        }

        // 3. Extract Raw Lines & Clean Redundant Sender Prefixes
        val rawLines = extractRawLines(extras)
        val cleanedLines = rawLines
            .map { cleanLinePrefix(it, senderName, rawTitle) }
            .filter { it.isNotBlank() }

        // Filter out generic summary headers like "Bạn có \d+ tin nhắn mới" if actual message lines are present
        val nonSummaryLines = cleanedLines.filterNot { summaryRegex.containsMatchIn(it) }
        val linesToProcess = if (nonSummaryLines.isNotEmpty()) nonSummaryLines else cleanedLines

        if (linesToProcess.isEmpty() && rawTitle.isBlank()) {
            return
        }

        val rawCombinedContent = if (linesToProcess.isNotEmpty()) {
            linesToProcess.joinToString("\n")
        } else {
            rawTitle
        }

        // 4. Video Message Detection
        val isVideo = checkIsVideoMessage(rawTitle, rawCombinedContent)

        // 5. Recall Detection (True Recall Only: explicit recall notice from app)
        val isRecall = checkIsRecallNotice(rawCombinedContent) || linesToProcess.any { checkIsRecallNotice(it) }
        if (isRecall) {
            val updatedRows = dao.markLatestSenderMessageAsRemoved(pkg, senderName, sbn.postTime)
            activeUnreadStacks.remove("$pkg|$senderName")
            if (updatedRows > 0) {
                // Successfully marked the previous message as recalled!
                // Do NOT insert the redundant recall text as a new card.
                return
            }
            // If updatedRows == 0, no prior message existed in DB, insert as a recalled notification
        }

        // 6. Delta Extraction for Stacked Unread Messages
        val stackKey = "$pkg|$senderName"
        val recentDbItems = dao.getRecentNotificationsForSender(pkg, senderName, 10)
        val recentDbContents = recentDbItems.map { it.content.trim() }
        val previousStack = activeUnreadStacks[stackKey] ?: emptyList()
        activeUnreadStacks[stackKey] = linesToProcess

        val newLinesToLog: List<String>
        if (linesToProcess.size <= 1) {
            val singleLine = linesToProcess.firstOrNull() ?: rawCombinedContent
            val latestDb = recentDbItems.firstOrNull()
            if (latestDb != null &&
                latestDb.content.trim() == singleLine.trim() &&
                kotlin.math.abs(sbn.postTime - latestDb.timestamp) < 3000L
            ) {
                return // Debounce duplicate
            }
            newLinesToLog = listOf(singleLine)
        } else {
            // Multi-line stacked unread messages
            if (previousStack.isNotEmpty() &&
                linesToProcess.size > previousStack.size &&
                linesToProcess.take(previousStack.size) == previousStack
            ) {
                // Case A: newly appended lines since previous notification
                newLinesToLog = linesToProcess.drop(previousStack.size)
            } else {
                // Case B: drop leading lines that already exist in previousStack or recent DB
                newLinesToLog = linesToProcess.dropWhile { line ->
                    line in previousStack || line in recentDbContents
                }
            }
            if (newLinesToLog.isEmpty()) {
                return // All lines were already logged in previous notifications
            }
        }

        var content = newLinesToLog.joinToString("\n").trim()
        if (content.isBlank() && rawTitle.isBlank()) {
            return
        }
        if (content.isBlank()) {
            content = rawTitle
        }

        if (isVideo) {
            val lang = PreferenceManager.getInstance(this).currentLanguage
            val videoNotice = if (lang == AppLanguage.VI) {
                "🎥 $senderName đã gửi 1 video"
            } else {
                "🎥 $senderName sent a video"
            }
            if (content.isBlank() ||
                content.equals("[Video]", ignoreCase = true) ||
                content.equals("video", ignoreCase = true)
            ) {
                content = videoNotice
            }
        }

        // 7. Extract Sender Avatar (64x64 WebP)
        val avatarKeyHash = StorageHelper.computeUniqueHash(pkg, senderName, "", 0L).take(16)
        var avatarPath: String? = null

        @Suppress("DEPRECATION")
        val directLargeIcon = notif.largeIcon
        if (directLargeIcon != null) {
            avatarPath = StorageHelper.saveSenderAvatarWebP(this, pkg, avatarKeyHash, directLargeIcon)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notif.getLargeIcon() != null) {
            val icon = notif.getLargeIcon()
            val drawable = try {
                icon?.loadDrawable(this)
            } catch (e: Exception) {
                null
            }
            if (drawable != null) {
                avatarPath = StorageHelper.saveSenderAvatarWebP(this, pkg, avatarKeyHash, drawable)
            }
        } else {
            @Suppress("DEPRECATION")
            val avatarBmp = extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)
            if (avatarBmp != null) {
                avatarPath = StorageHelper.saveSenderAvatarWebP(this, pkg, avatarKeyHash, avatarBmp)
            }
        }

        // 8. Image Attachment: Always null (removed non-functional feature per Phase 6)
        val imagePath: String? = null

        // 9. Deterministic Unique Hash & Room Insertion
        val uniqueHash = StorageHelper.computeUniqueHash(pkg, senderName, content, sbn.postTime)
        val entity = NotificationEntity(
            uniqueHash = uniqueHash,
            sbnKey = sbn.key,
            packageName = pkg,
            appName = meta.appName,
            senderName = senderName,
            title = rawTitle.ifEmpty { senderName },
            content = content,
            timestamp = sbn.postTime,
            isRemoved = isRecall,
            removedAt = if (isRecall) sbn.postTime else null,
            isStarred = false,
            isVideoMessage = isVideo,
            senderAvatarPath = avatarPath,
            imageAttachmentPath = imagePath
        )
        dao.insertNotification(entity)
    }

    private fun cleanLinePrefix(line: String, senderName: String, rawTitle: String): String {
        var trimmed = line.trim()
        val prefixes = mutableListOf<String>()
        if (senderName.isNotBlank()) prefixes.add(senderName)
        if (rawTitle.isNotBlank() && rawTitle != senderName) prefixes.add(rawTitle)
        if (senderName.contains(" • ")) {
            val partAfter = senderName.substringAfter(" • ").trim()
            if (partAfter.isNotBlank()) prefixes.add(partAfter)
            val partBefore = senderName.substringBefore(" • ").trim()
            if (partBefore.isNotBlank()) prefixes.add(partBefore)
        }

        for (p in prefixes) {
            val colonSpace = "$p: "
            val colon = "$p:"
            if (trimmed.startsWith(colonSpace, ignoreCase = true)) {
                trimmed = trimmed.substring(colonSpace.length).trim()
            } else if (trimmed.startsWith(colon, ignoreCase = true)) {
                trimmed = trimmed.substring(colon.length).trim()
            }
        }
        return trimmed
    }

    private fun extractRawLines(extras: Bundle): List<String> {
        val lines = mutableListOf<String>()

        // 1. MessagingStyle
        @Suppress("DEPRECATION")
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (!messages.isNullOrEmpty()) {
            for (item in messages) {
                if (item is Bundle) {
                    val text = item.getCharSequence("text")?.toString()?.trim()
                    if (!text.isNullOrBlank()) {
                        lines.add(text)
                    }
                }
            }
            if (lines.isNotEmpty()) return lines
        }

        // 2. InboxStyle (EXTRA_TEXT_LINES)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!textLines.isNullOrEmpty()) {
            for (tl in textLines) {
                val str = tl?.toString()?.trim()
                if (!str.isNullOrBlank()) {
                    lines.add(str)
                }
            }
            if (lines.isNotEmpty()) return lines
        }

        // 3. EXTRA_BIG_TEXT or EXTRA_TEXT
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val fallback = bigText?.ifBlank { null } ?: text?.ifBlank { null } ?: ""
        if (fallback.isNotBlank()) {
            lines.addAll(fallback.split("\n").map { it.trim() }.filter { it.isNotBlank() })
        }

        return lines
    }

    private fun checkIsVideoMessage(title: String, content: String): Boolean {
        val combined = "$title $content".lowercase(Locale.ROOT)
        return combined.contains("[video]") ||
                combined.contains("đã gửi một video") ||
                combined.contains("đã gửi 1 video") ||
                combined.contains("gửi video") ||
                combined.contains("sent a video") ||
                combined.contains("🎥")
    }

    private fun checkIsRecallNotice(content: String): Boolean {
        val lower = content.lowercase(Locale.ROOT)
        return lower.contains("đã thu hồi một tin nhắn") ||
                lower.contains("đã thu hồi tin nhắn") ||
                lower.contains("tin nhắn đã bị thu hồi") ||
                lower.contains("vừa thu hồi một tin nhắn") ||
                lower.contains("thu hồi một tin nhắn") ||
                lower.contains("thu hồi tin nhắn") ||
                lower.contains("unsent a message") ||
                lower.contains("removed a message") ||
                lower.contains("recalled a message")
    }

    private fun startSilentForeground() {
        val prefs = PreferenceManager.getInstance(this)
        if (!prefs.isBackgroundEnabled || !prefs.isForegroundSilentNotifEnabled) {
            return
        }

        createNotificationChannelIfNeeded()

        val currentLang = prefs.currentLanguage
        val title = if (currentLang == AppLanguage.VI) {
            "SNL Demo đang hoạt động"
        } else {
            "SNL Demo is active"
        }
        val text = if (currentLang == AppLanguage.VI) {
            "Đang lưu lịch sử thông báo ngầm (100% Offline)"
        } else {
            "Logging notifications in background (100% Offline)"
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    FOREGROUND_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FOREGROUND_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(FOREGROUND_ID, notification)
            }
        } catch (e: Exception) {
            try {
                startForeground(FOREGROUND_ID, notification)
            } catch (inner: Exception) {
                inner.printStackTrace()
            }
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notification Logger Silent Keep-Alive",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains persistent offline notification logging"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "snl_keep_alive_channel"
        const val FOREGROUND_ID = 1001
        const val ACTION_START_SERVICE = "com.local.snldemo.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.local.snldemo.action.STOP_SERVICE"
    }
}
