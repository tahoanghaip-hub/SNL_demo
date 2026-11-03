package com.local.snldemo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.local.snldemo.data.local.AppDatabase
import com.local.snldemo.data.local.AppMetadataEntity
import com.local.snldemo.data.local.AppSummaryProjection
import com.local.snldemo.data.local.NotificationEntity
import com.local.snldemo.util.BackupRestoreHelper
import com.local.snldemo.util.PreferenceManager
import com.local.snldemo.util.ServiceControlHelper
import com.local.snldemo.util.StorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NotificationFilterState(
    val query: String = "",
    val appFilter: String? = null,
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val onlyStarred: Boolean = false,
    val onlyRemoved: Boolean = false
) {
    val isAnyActive: Boolean
        get() = query.isNotBlank() || appFilter != null || startMillis != null || endMillis != null || onlyStarred || onlyRemoved
}

data class DeviceStorageUiState(
    val totalDeviceBytes: Long = 0L,
    val usedDeviceBytes: Long = 0L,
    val freeDeviceBytes: Long = 0L,
    val totalAppOccupiedBytes: Long = 0L
)

data class AppStorageDetail(
    val packageName: String,
    val appName: String,
    val iconPath: String?,
    val notificationCount: Int,
    val removedCount: Int,
    val textBytes: Long,
    val mediaBytes: Long,
    val totalBytes: Long
)

data class DailyTrafficPoint(
    val dateLabel: String,
    val count: Int,
    val dayStartMillis: Long
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferenceManager = PreferenceManager.getInstance(application)
    private val database = AppDatabase.getInstance(application)
    private val dao = database.notificationDao()

    // 1. Language & Preferences State
    private val _languageCode = MutableStateFlow(preferenceManager.languageCode)
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    fun setLanguage(code: String) {
        preferenceManager.languageCode = code
        _languageCode.value = code
    }

    // 2. Data Flows from Room
    val appSummaries: StateFlow<List<AppSummaryProjection>> = dao.observeAppSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appMetadataMap: StateFlow<Map<String, AppMetadataEntity>> = dao.observeAllAppMetadata()
        .map { list -> list.associateBy { it.packageName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allNotifications: StateFlow<List<NotificationEntity>> = dao.observeAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Search & Multi-Filter State
    val searchQuery = MutableStateFlow("")
    val selectedAppFilter = MutableStateFlow<String?>(null) // null = All Apps
    val startDateMillis = MutableStateFlow<Long?>(null) // Date range start
    val endDateMillis = MutableStateFlow<Long?>(null) // Date range end
    val onlyStarred = MutableStateFlow(false)
    val onlyRemoved = MutableStateFlow(false)

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedAppFilter(pkg: String?) {
        selectedAppFilter.value = pkg
    }

    fun setDateRange(start: Long?, end: Long?) {
        startDateMillis.value = start
        endDateMillis.value = end
    }

    fun toggleStarred() {
        onlyStarred.value = !onlyStarred.value
    }

    fun toggleRemoved() {
        onlyRemoved.value = !onlyRemoved.value
    }

    fun clearAllFilters() {
        searchQuery.value = ""
        selectedAppFilter.value = null
        startDateMillis.value = null
        endDateMillis.value = null
        onlyStarred.value = false
        onlyRemoved.value = false
    }

    // Combine individual filter flows into a single NotificationFilterState
    private val filterStateFlow = combine(
        combine(searchQuery, selectedAppFilter) { q, app -> Pair(q, app) },
        combine(startDateMillis, endDateMillis) { start, end -> Pair(start, end) },
        combine(onlyStarred, onlyRemoved) { starred, removed -> Pair(starred, removed) }
    ) { (q, app), (start, end), (starred, removed) ->
        NotificationFilterState(
            query = q,
            appFilter = app,
            startMillis = start,
            endMillis = end,
            onlyStarred = starred,
            onlyRemoved = removed
        )
    }

    val isAnyFilterActive: StateFlow<Boolean> = filterStateFlow
        .map { it.isAnyActive }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Combined multi-filter notification stream
    val filteredNotifications: StateFlow<List<NotificationEntity>> = combine(
        allNotifications,
        filterStateFlow
    ) { notifs, filter ->
        notifs.filter { item ->
            // App filter
            if (filter.appFilter != null && item.packageName != filter.appFilter) {
                return@filter false
            }
            // Starred filter
            if (filter.onlyStarred && !item.isStarred) {
                return@filter false
            }
            // Removed filter
            if (filter.onlyRemoved && !item.isRemoved) {
                return@filter false
            }
            // Date range filter
            if (filter.startMillis != null && item.timestamp < filter.startMillis) {
                return@filter false
            }
            if (filter.endMillis != null && item.timestamp > filter.endMillis) {
                return@filter false
            }
            // Search query filter (matches title, content, senderName, or appName)
            if (filter.query.isNotBlank()) {
                val q = filter.query.trim()
                val matchTitle = item.title.contains(q, ignoreCase = true)
                val matchContent = item.content.contains(q, ignoreCase = true)
                val matchSender = item.senderName.contains(q, ignoreCase = true)
                val matchApp = item.appName.contains(q, ignoreCase = true)
                if (!matchTitle && !matchContent && !matchSender && !matchApp) {
                    return@filter false
                }
            }
            true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. On-Demand Storage & Analytics State
    val deviceStorageState = MutableStateFlow(DeviceStorageUiState())
    val appStorageDetails = MutableStateFlow<List<AppStorageDetail>>(emptyList())
    val last7DaysTraffic = MutableStateFlow<List<DailyTrafficPoint>>(emptyList())
    val isCalculatingStorage = MutableStateFlow(false)

    fun refreshAnalyticsAndStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            isCalculatingStorage.value = true
            try {
                val context = getApplication<Application>()
                val (totalDev, usedDev, freeDev) = StorageHelper.getDeviceStorageInfo()
                val totalAppBytes = StorageHelper.getDatabaseTotalBytes(context)
                deviceStorageState.value = DeviceStorageUiState(
                    totalDeviceBytes = totalDev,
                    usedDeviceBytes = usedDev,
                    freeDeviceBytes = freeDev,
                    totalAppOccupiedBytes = totalAppBytes
                )

                // Fetch app summaries and metadata map
                val currentSummaries = appSummaries.value.ifEmpty {
                    dao.observeAppSummaries().firstOrNull() ?: emptyList()
                }
                val metaMap = appMetadataMap.value.ifEmpty {
                    dao.getAllAppMetadataSync().associateBy { it.packageName }
                }

                val details = currentSummaries.map { summary ->
                    val mediaBytes = StorageHelper.getAppMediaBytes(context, summary.packageName)
                    val totalBytes = summary.textBytes + mediaBytes
                    val meta = metaMap[summary.packageName]
                    AppStorageDetail(
                        packageName = summary.packageName,
                        appName = summary.appName,
                        iconPath = meta?.appIconPath,
                        notificationCount = summary.totalCount,
                        removedCount = summary.removedCount,
                        textBytes = summary.textBytes,
                        mediaBytes = mediaBytes,
                        totalBytes = totalBytes
                    )
                }.sortedByDescending { it.totalBytes }

                appStorageDetails.value = details

                // Compute last 7 days traffic
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val notifs = allNotifications.value
                val traffic = mutableListOf<DailyTrafficPoint>()
                val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val dayCal = (calendar.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, -i)
                    }
                    val startMillis = dayCal.timeInMillis
                    val endMillis = startMillis + (24L * 60L * 60L * 1000L) - 1L
                    val count = notifs.count { it.timestamp in startMillis..endMillis }
                    traffic.add(
                        DailyTrafficPoint(
                            dateLabel = sdf.format(Date(startMillis)),
                            count = count,
                            dayStartMillis = startMillis
                        )
                    )
                }
                last7DaysTraffic.value = traffic
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCalculatingStorage.value = false
            }
        }
    }

    // 5. Flexible Per-App Cleanup & VACUUM Operations
    fun cleanAllForApp(packageName: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val imagePaths = dao.getImagePathsForApp(packageName)
            StorageHelper.deleteFiles(imagePaths)
            StorageHelper.deleteAppImageFolder(context, packageName)
            dao.deleteAllForApp(packageName)
            database.performVacuum()
            refreshAnalyticsAndStorage()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun cleanOlderThanDaysForApp(packageName: String, days: Int, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
            val imagePaths = dao.getImagePathsOlderThan(packageName, cutoffTime)
            StorageHelper.deleteFiles(imagePaths)
            dao.deleteOlderThan(packageName, cutoffTime)
            database.performVacuum()
            refreshAnalyticsAndStorage()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun cleanUnstarredForApp(packageName: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val imagePaths = dao.getUnstarredImagePaths(packageName)
            StorageHelper.deleteFiles(imagePaths)
            dao.deleteUnstarredForApp(packageName)
            database.performVacuum()
            refreshAnalyticsAndStorage()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun triggerManualVacuum(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            database.performVacuum()
            refreshAnalyticsAndStorage()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    init {
        // Automatically sync installed app names and icons into app_metadata on startup
        viewModelScope.launch {
            ServiceControlHelper.syncInstalledAppsToMetadata(getApplication())
        }
    }

    // 6. Quick Action Methods
    fun toggleStar(notification: NotificationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setStarred(notification.id, !notification.isStarred)
        }
    }

    fun deleteSingleNotification(notification: NotificationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!notification.imageAttachmentPath.isNullOrBlank()) {
                StorageHelper.deleteFiles(listOf(notification.imageAttachmentPath))
            }
            dao.deleteNotificationById(notification.id)
        }
    }

    fun saveImageToGallery(imagePath: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = StorageHelper.copyImageToPublicGallery(getApplication(), imagePath)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun openSourceApp(packageName: String): Boolean {
        return ServiceControlHelper.openTargetApp(getApplication(), packageName)
    }

    // 7. System & Service State
    val isBackgroundEnabled = MutableStateFlow(preferenceManager.isBackgroundEnabled)
    val isSilentNotifEnabled = MutableStateFlow(preferenceManager.isForegroundSilentNotifEnabled)
    val isSyncingInstalledApps = MutableStateFlow(false)
    val isBackupInProgress = MutableStateFlow(false)

    fun toggleBackgroundMaster(enabled: Boolean) {
        ServiceControlHelper.setBackgroundServiceEnabled(getApplication(), enabled)
        isBackgroundEnabled.value = enabled
    }

    fun toggleSilentForegroundNotif(enabled: Boolean) {
        preferenceManager.isForegroundSilentNotifEnabled = enabled
        isSilentNotifEnabled.value = enabled
        ServiceControlHelper.ensureServiceActive(getApplication())
    }

    // 8. Per-App Filter & Save-Image Controls
    fun syncInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            isSyncingInstalledApps.value = true
            try {
                ServiceControlHelper.syncInstalledAppsToMetadata(getApplication())
            } finally {
                isSyncingInstalledApps.value = false
            }
        }
    }

    fun setAppTrackingEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setAppTracking(packageName, enabled)
        }
    }

    fun setAppSaveImageEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setAppSaveImage(packageName, enabled)
        }
    }

    // 9. Backup & Restore Wrappers
    fun exportFullBackup(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isBackupInProgress.value = true
            val success = BackupRestoreHelper.exportFullBackupToUri(getApplication(), uri)
            refreshAnalyticsAndStorage()
            isBackupInProgress.value = false
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun importFullBackup(uri: Uri, onComplete: (BackupRestoreHelper.RestoreResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isBackupInProgress.value = true
            val result = BackupRestoreHelper.importAndMergeBackupFromUri(getApplication(), uri)
            refreshAnalyticsAndStorage()
            isBackupInProgress.value = false
            withContext(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }

    fun exportCsv(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isBackupInProgress.value = true
            val success = BackupRestoreHelper.exportCsvToUri(getApplication(), uri)
            isBackupInProgress.value = false
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }
}
