package com.local.snldemo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.local.snldemo.data.local.AppDatabase
import com.local.snldemo.data.local.AppMetadataEntity
import com.local.snldemo.data.local.AppSummaryProjection
import com.local.snldemo.data.local.NotificationEntity
import com.local.snldemo.util.PreferenceManager
import com.local.snldemo.util.ServiceControlHelper
import com.local.snldemo.util.StorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferenceManager = PreferenceManager.getInstance(application)
    private val dao = AppDatabase.getInstance(application).notificationDao()

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

    init {
        // Automatically sync installed app names and icons into app_metadata on startup
        viewModelScope.launch {
            ServiceControlHelper.syncInstalledAppsToMetadata(getApplication())
        }
    }

    // 4. Quick Action Methods
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
}
