package com.local.snldemo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.local.snldemo.R

class NotifLoggerService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification()
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Handled in Phase 2: Notification Ingestion Pipeline, deduplication & Room insertion
        if (sbn == null) return
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Handled in Phase 2: Unsent / Cancellation tracking by sbnKey
        if (sbn == null) return
    }

    private fun startForegroundServiceNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SNL Demo")
            .setContentText("Offline notification logging active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notification Logger Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the offline notification logger running continuously"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "snl_service_channel"
        const val NOTIFICATION_ID = 1001
    }
}

