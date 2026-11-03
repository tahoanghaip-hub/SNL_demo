package com.local.snldemo.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Dao
interface NotificationDao {

    // --- Zero-Overhead Background Operations ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(entity: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotificationsBatch(entities: List<NotificationEntity>): List<Long>

    @Query("UPDATE notifications SET isRemoved = 1, removedAt = :removedAt WHERE sbnKey = :sbnKey AND isRemoved = 0")
    suspend fun markAsRemovedByKey(sbnKey: String, removedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppMetadata(meta: AppMetadataEntity)

    @Query("SELECT * FROM app_metadata WHERE packageName = :pkg LIMIT 1")
    suspend fun getAppMetadataSync(pkg: String): AppMetadataEntity?

    @Query("SELECT * FROM notifications WHERE packageName = :pkg ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestNotificationForApp(pkg: String): NotificationEntity?

    // --- Foreground UI & On-Demand Storage Queries ---

    @Query("SELECT * FROM app_metadata ORDER BY appName ASC")
    fun observeAllAppMetadata(): Flow<List<AppMetadataEntity>>

    @Query("SELECT * FROM app_metadata")
    suspend fun getAllAppMetadataSync(): List<AppMetadataEntity>

    @Query("""
        SELECT packageName,
               appName,
               CAST(COUNT(id) AS INTEGER) as totalCount,
               CAST(COALESCE(SUM(CASE WHEN isRemoved = 1 THEN 1 ELSE 0 END), 0) AS INTEGER) as removedCount,
               COALESCE(MAX(timestamp), 0) as lastTimestamp,
               COALESCE(SUM(LENGTH(title) + LENGTH(content) + LENGTH(senderName)), 0) as textBytes
        FROM notifications
        GROUP BY packageName
        ORDER BY lastTimestamp DESC
    """)
    fun observeAppSummaries(): Flow<List<AppSummaryProjection>>

    @Query("SELECT * FROM notifications WHERE packageName = :pkg ORDER BY timestamp DESC")
    fun observeNotificationsForApp(pkg: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    suspend fun getAllNotificationsSync(): List<NotificationEntity>

    @Query("UPDATE notifications SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean)

    @Query("UPDATE app_metadata SET isTrackingEnabled = :enabled WHERE packageName = :pkg")
    suspend fun setAppTracking(pkg: String, enabled: Boolean)

    @Query("UPDATE app_metadata SET isSaveImageEnabled = :enabled WHERE packageName = :pkg")
    suspend fun setAppSaveImage(pkg: String, enabled: Boolean)

    // --- Flexible Deletion & Cleanup Queries ---

    @Query("SELECT imageAttachmentPath FROM notifications WHERE packageName = :pkg AND imageAttachmentPath IS NOT NULL")
    suspend fun getImagePathsForApp(pkg: String): List<String>

    @Query("DELETE FROM notifications WHERE packageName = :pkg")
    suspend fun deleteAllForApp(pkg: String)

    @Query("SELECT imageAttachmentPath FROM notifications WHERE packageName = :pkg AND timestamp < :cutoffTime AND imageAttachmentPath IS NOT NULL")
    suspend fun getImagePathsOlderThan(pkg: String, cutoffTime: Long): List<String>

    @Query("DELETE FROM notifications WHERE packageName = :pkg AND timestamp < :cutoffTime")
    suspend fun deleteOlderThan(pkg: String, cutoffTime: Long)

    @Query("SELECT imageAttachmentPath FROM notifications WHERE packageName = :pkg AND isStarred = 0 AND imageAttachmentPath IS NOT NULL")
    suspend fun getUnstarredImagePaths(pkg: String): List<String>

    @Query("DELETE FROM notifications WHERE packageName = :pkg AND isStarred = 0")
    suspend fun deleteUnstarredForApp(pkg: String)

    @RawQuery
    suspend fun checkpointAndVacuum(query: SupportSQLiteQuery): Int
}

@Database(
    entities = [NotificationEntity::class, AppMetadataEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao

    suspend fun performVacuum() {
        withContext(Dispatchers.IO) {
            val db = openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(FULL);").use { cursor ->
                cursor.moveToFirst()
            }
            db.execSQL("VACUUM;")
        }
    }

    companion object {
        const val DATABASE_NAME = "snl_local.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { INSTANCE = it }
            }
        }
    }
}

