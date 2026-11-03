package com.local.snldemo.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String) {
    VI("vi", "Tiếng Việt"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: VI
        }
    }
}

data class Strings(
    // Navigation Tabs
    val tabHistory: String,
    val tabAnalytics: String,
    val tabSettings: String,

    // Common Labels & Filters
    val searchHint: String,
    val allTimelineTab: String,
    val conversationsTab: String,
    val filterDateRange: String,
    val filterStarred: String,
    val filterRemoved: String,
    val clearFilters: String,
    val noNotifications: String,

    // Badges & Media
    val badgeRemoved: String,
    val sentAVideo: String,
    val imageAttached: String,

    // Quick Actions
    val actionCopy: String,
    val actionCopiedToast: String,
    val actionSaveImage: String,
    val actionSavedImageToast: String,
    val actionOpenApp: String,
    val actionStar: String,
    val actionDelete: String,

    // Analytics & Storage Labels
    val deviceStorageTitle: String,
    val totalSpace: String,
    val usedSpace: String,
    val freeSpace: String,
    val appDbSize: String,
    val pieChartTitle: String,
    val barChartTitle: String,
    val storageByAppTitle: String,
    val deleteAllForApp: String,
    val deleteOlderThanDays: String,
    val deleteKeepStarred: String,
    val vacuumSuccessToast: String,

    // Settings & System Health Labels
    val systemHealthTitle: String,
    val permNotifAccess: String,
    val permBatteryOpt: String,
    val serviceRunningState: String,
    val statusGranted: String,
    val statusMissing: String,
    val backgroundServiceToggle: String,
    val languageToggleTitle: String,
    val appFilterTitle: String,
    val allowLogToggle: String,
    val allowSaveImageToggle: String,
    val backupRestoreTitle: String,
    val btnExportBackup: String,
    val btnImportBackup: String,
    val btnExportCsv: String,

    // Supplementary Dialog & Action Labels for Seamless Phase 2-5 UX
    val dialogConfirmTitle: String,
    val dialogConfirmDelete: String,
    val btnCancel: String,
    val btnConfirm: String,
    val filterAllApps: String,
    val backupExportSuccess: String,
    val backupImportSuccess: String,
    val csvExportSuccess: String,
    val vacuumDbButton: String,
    val vacuumDbDesc: String,
    val backToConversations: String = "← Quay lại hội thoại",
    val noConversations: String = "Không tìm thấy hội thoại nào",
    val selectDateRange: String = "Chọn khoảng thời gian",
    val messagesCount: String = "%d thông báo"
)

val VietnameseStrings = Strings(
    // Navigation Tabs
    tabHistory = "Lịch sử",
    tabAnalytics = "Thống kê",
    tabSettings = "Cài đặt",

    // Common Labels & Filters
    searchHint = "Tìm kiếm thông báo, người gửi, nội dung...",
    allTimelineTab = "Tất cả",
    conversationsTab = "Hội thoại",
    filterDateRange = "Khoảng thời gian",
    filterStarred = "Đã ghim",
    filterRemoved = "Đã thu hồi",
    clearFilters = "Xóa bộ lọc",
    noNotifications = "Chưa có thông báo nào được ghi lại",

    // Badges & Media
    badgeRemoved = "[Đã bị thu hồi]",
    sentAVideo = "🎥 %s đã gửi 1 video",
    imageAttached = "[Hình ảnh đính kèm]",

    // Quick Actions
    actionCopy = "Sao chép",
    actionCopiedToast = "Đã sao chép vào bộ nhớ tạm",
    actionSaveImage = "Lưu ảnh",
    actionSavedImageToast = "Đã lưu ảnh vào Thư viện (Pictures/SNL_Saved)",
    actionOpenApp = "Mở ứng dụng",
    actionStar = "Ghim",
    actionDelete = "Xóa",

    // Analytics & Storage Labels
    deviceStorageTitle = "Dung lượng thiết bị",
    totalSpace = "Tổng dung lượng",
    usedSpace = "Đã sử dụng",
    freeSpace = "Còn trống",
    appDbSize = "Dung lượng ứng dụng & CSDL",
    pieChartTitle = "Phân bổ lưu trữ theo ứng dụng",
    barChartTitle = "Tần suất thông báo theo ngày",
    storageByAppTitle = "Dung lượng theo ứng dụng",
    deleteAllForApp = "Xóa toàn bộ thông báo của ứng dụng",
    deleteOlderThanDays = "Xóa thông báo cũ hơn (ngày)",
    deleteKeepStarred = "Giữ lại thông báo đã ghim",
    vacuumSuccessToast = "Đã dọn dẹp và tối ưu CSDL thành công",

    // Settings & System Health Labels
    systemHealthTitle = "Tình trạng hệ thống",
    permNotifAccess = "Quyền truy cập thông báo",
    permBatteryOpt = "Bỏ qua tối ưu hóa pin",
    serviceRunningState = "Trạng thái Dịch vụ ngầm",
    statusGranted = "Đã cấp",
    statusMissing = "Chưa cấp",
    backgroundServiceToggle = "Bật ghi nhận thông báo chạy ngầm",
    languageToggleTitle = "Ngôn ngữ giao diện",
    appFilterTitle = "Bộ lọc ứng dụng theo dõi",
    allowLogToggle = "Cho phép ghi nhận",
    allowSaveImageToggle = "Lưu hình ảnh đính kèm",
    backupRestoreTitle = "Sao lưu & Khôi phục (Offline)",
    btnExportBackup = "Xuất file sao lưu (JSON)",
    btnImportBackup = "Nhập khôi phục (Merge)",
    btnExportCsv = "Xuất dữ liệu CSV",

    // Supplementary Dialog & Action Labels
    dialogConfirmTitle = "Xác nhận hành động",
    dialogConfirmDelete = "Bạn có chắc chắn muốn xóa dữ liệu đã chọn? Hành động này không thể hoàn tác.",
    btnCancel = "Hủy",
    btnConfirm = "Xác nhận",
    filterAllApps = "Tất cả ứng dụng",
    backupExportSuccess = "Đã xuất file sao lưu thành công",
    backupImportSuccess = "Đã khôi phục dữ liệu thành công",
    csvExportSuccess = "Đã xuất file CSV thành công",
    vacuumDbButton = "Dọn dẹp CSDL (Vacuum)",
    vacuumDbDesc = "Giải phóng không gian đĩa từ các bản ghi và hình ảnh đã bị xóa"
)

val EnglishStrings = Strings(
    // Navigation Tabs
    tabHistory = "History",
    tabAnalytics = "Analytics",
    tabSettings = "Settings",

    // Common Labels & Filters
    searchHint = "Search notifications, sender, content...",
    allTimelineTab = "All",
    conversationsTab = "Conversations",
    filterDateRange = "Date Range",
    filterStarred = "Starred",
    filterRemoved = "Removed",
    clearFilters = "Clear Filters",
    noNotifications = "No notifications recorded yet",

    // Badges & Media
    badgeRemoved = "[Unsent/Removed]",
    sentAVideo = "🎥 %s sent a video",
    imageAttached = "[Image Attached]",

    // Quick Actions
    actionCopy = "Copy",
    actionCopiedToast = "Copied to clipboard",
    actionSaveImage = "Save Image",
    actionSavedImageToast = "Saved image to Gallery (Pictures/SNL_Saved)",
    actionOpenApp = "Open App",
    actionStar = "Star",
    actionDelete = "Delete",

    // Analytics & Storage Labels
    deviceStorageTitle = "Device Storage",
    totalSpace = "Total Space",
    usedSpace = "Used Space",
    freeSpace = "Free Space",
    appDbSize = "App & Database Storage",
    pieChartTitle = "Storage Distribution by App",
    barChartTitle = "Notification Frequency by Day",
    storageByAppTitle = "Storage by Application",
    deleteAllForApp = "Delete all notifications for app",
    deleteOlderThanDays = "Delete older than (days)",
    deleteKeepStarred = "Keep starred notifications",
    vacuumSuccessToast = "Database vacuumed and optimized successfully",

    // Settings & System Health Labels
    systemHealthTitle = "System Health",
    permNotifAccess = "Notification Access Permission",
    permBatteryOpt = "Ignore Battery Optimization",
    serviceRunningState = "Background Service Status",
    statusGranted = "Granted",
    statusMissing = "Missing",
    backgroundServiceToggle = "Enable Background Notification Logging",
    languageToggleTitle = "Display Language",
    appFilterTitle = "Tracked Apps Filter",
    allowLogToggle = "Allow Logging",
    allowSaveImageToggle = "Save Attached Images",
    backupRestoreTitle = "Backup & Restore (Offline)",
    btnExportBackup = "Export Backup (JSON)",
    btnImportBackup = "Import Backup (Merge)",
    btnExportCsv = "Export to CSV",

    // Supplementary Dialog & Action Labels
    dialogConfirmTitle = "Confirm Action",
    dialogConfirmDelete = "Are you sure you want to delete the selected data? This action cannot be undone.",
    btnCancel = "Cancel",
    btnConfirm = "Confirm",
    filterAllApps = "All Apps",
    backupExportSuccess = "Backup file exported successfully",
    backupImportSuccess = "Data restored and merged successfully",
    csvExportSuccess = "CSV file exported successfully",
    vacuumDbButton = "Vacuum Database",
    vacuumDbDesc = "Reclaim freed disk space from deleted records and images",
    backToConversations = "← Back to conversations",
    noConversations = "No conversations found",
    selectDateRange = "Select Date Range",
    messagesCount = "%d notifications"
)

val LocalAppStrings = staticCompositionLocalOf { VietnameseStrings }

fun getStrings(language: AppLanguage): Strings {
    return when (language) {
        AppLanguage.VI -> VietnameseStrings
        AppLanguage.EN -> EnglishStrings
    }
}

