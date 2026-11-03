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

class NotifLoggerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        // Deep Content & Media Extraction on Dispatchers.IO (Zero UI/Main thread overhead)
        serviceScope.launch {
            try {
                processNotificationPosted(sbn, notif, pkg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        if (sbn == null) return
        val prefs = PreferenceManager.getInstance(this)
        if (!prefs.isBackgroundEnabled) return

        // Only mark as removed if the notification was cancelled by the source app itself
        // (REASON_CLICK, REASON_CANCEL, REASON_CANCEL_ALL are user swipe/tap actions and NOT recalls)
        val isAppRecalled = (reason == REASON_APP_CANCEL ||
                reason == REASON_APP_CANCEL_ALL ||
                reason == REASON_GROUP_SUMMARY_CANCELED)

        if (isAppRecalled) {
            val sbnKey = sbn.key
            val removedAt = System.currentTimeMillis()
            serviceScope.launch {
                val dao = AppDatabase.getInstance(this@NotifLoggerService).notificationDao()
                dao.markAsRemovedByKey(sbnKey, removedAt)
            }
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
            return // App is blacklisted/disabled by user
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

        // 3. Extract Richest Content Representation
        var content = extractRichestContent(extras)
        if (rawTitle.isBlank() && content.isBlank()) {
            return
        }

        // 4. Video Message Detection
        val isVideo = checkIsVideoMessage(rawTitle, content)
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

        // 5. Anti-Duplicate Debounce Check
        val latest = dao.getLatestNotificationForApp(pkg)
        if (latest != null) {
            val isDuplicate = latest.title == rawTitle &&
                    latest.content == content &&
                    latest.senderName == senderName &&
                    kotlin.math.abs(sbn.postTime - latest.timestamp) < 2500L
            if (isDuplicate) {
                return
            }
        }

        // 6. In-Place Unsent Detection (Zalo/Messenger style in-place recall notice)
        val isRecallNotice = checkIsRecallNotice(content)
        if (isRecallNotice) {
            dao.markAsRemovedByKey(sbn.key, System.currentTimeMillis())
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

        // 8. Extract Image Attachment (if enabled for this app)
        var imagePath: String? = null
        if (meta.isSaveImageEnabled) {
            @Suppress("DEPRECATION")
            val pictureBitmap = extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
                ?: extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON_BIG)

            if (pictureBitmap != null) {
                imagePath = StorageHelper.saveNotificationImageWebP(this, pkg, pictureBitmap)
            }
        }

        // 9. Compute Deterministic SHA-256 Hash & Insert to Room
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
            isRemoved = false,
            removedAt = null,
            isStarred = false,
            isVideoMessage = isVideo,
            senderAvatarPath = avatarPath,
            imageAttachmentPath = imagePath
        )
        dao.insertNotification(entity)
    }

    private fun extractRichestContent(extras: Bundle): String {
        val candidates = mutableListOf<String>()

        // 1. MessagingStyle messages
        val msgText = extractMessagingStyleText(extras)
        if (msgText.isNotBlank()) {
            candidates.add(msgText)
        }

        // 2. BigText
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        if (!bigText.isNullOrBlank()) {
            candidates.add(bigText)
        }

        // 3. TextLines (InboxStyle)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotBlank() }
            ?.joinToString("\n")
        if (!textLines.isNullOrBlank()) {
            candidates.add(textLines)
        }

        // 4. Regular Text
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            candidates.add(text)
        }

        return candidates.maxByOrNull { it.length } ?: ""
    }

    private fun extractMessagingStyleText(extras: Bundle): String {
        @Suppress("DEPRECATION")
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return ""
        if (messages.isEmpty()) return ""

        val sb = StringBuilder()
        for (item in messages) {
            if (item is Bundle) {
                val text = item.getCharSequence("text")?.toString()?.trim()
                val sender = item.getCharSequence("sender")?.toString()?.trim()
                if (!text.isNullOrBlank()) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    if (!sender.isNullOrBlank()) {
                        sb.append(sender).append(": ")
                    }
                    sb.append(text)
                }
            }
        }
        return sb.toString().trim()
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
        return lower.contains("thu hồi một tin nhắn") ||
                lower.contains("thu hồi tin nhắn") ||
                lower.contains("đã thu hồi") ||
                lower.contains("tin nhắn đã bị thu hồi") ||
                lower.contains("unsent a message") ||
                lower.contains("removed a message")
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
