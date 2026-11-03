package com.local.snldemo.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["uniqueHash"], unique = true),
        Index(value = ["packageName"]),
        Index(value = ["timestamp"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uniqueHash: String, // SHA-256 or deterministic hash of (packageName + senderName + content + timestamp) for conflict-free Merge Restore
    val sbnKey: String, // StatusBarNotification.key to track removal/unsent
    val packageName: String,
    val appName: String,
    val senderName: String, // Conversation/Group/Sender title
    val title: String,
    val content: String, // Full expanded text (bigText / textLines)
    val timestamp: Long,
    val isRemoved: Boolean = false, // True when unsent/cancelled by source app
    val removedAt: Long? = null,
    val isStarred: Boolean = false, // Bookmarked by user
    val isVideoMessage: Boolean = false, // True if detected as video message
    val senderAvatarPath: String? = null, // Local file path to 64x64 WebP avatar
    val imageAttachmentPath: String? = null // Local file path to compressed WebP picture
)

@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val appIconPath: String?, // Cached 64x64 WebP icon path so uninstalled apps still keep their icon
    val isTrackingEnabled: Boolean = true, // Whitelist/Blacklist filter
    val isSaveImageEnabled: Boolean = false, // Per-app setting to allow saving attached images
    val lastUpdated: Long = System.currentTimeMillis()
)

data class AppSummaryProjection(
    val packageName: String,
    val appName: String,
    val totalCount: Int,
    val removedCount: Int,
    val lastTimestamp: Long,
    val textBytes: Long
)

data class DailyCountProjection(
    val dayLabel: String,
    val count: Int
)

