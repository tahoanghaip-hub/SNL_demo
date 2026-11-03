package com.local.snldemo.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.local.snldemo.data.local.AppMetadataEntity
import com.local.snldemo.ui.MainViewModel
import com.local.snldemo.ui.components.LocalFileImage
import com.local.snldemo.ui.i18n.LocalAppStrings
import com.local.snldemo.util.BackupRestoreHelper
import com.local.snldemo.util.ServiceControlHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val languageCode by viewModel.languageCode.collectAsState()
    val isBackgroundEnabled by viewModel.isBackgroundEnabled.collectAsState()
    val isSilentNotifEnabled by viewModel.isSilentNotifEnabled.collectAsState()
    val isSyncingApps by viewModel.isSyncingInstalledApps.collectAsState()
    val isBackupRunning by viewModel.isBackupInProgress.collectAsState()
    val appMetadataMap by viewModel.appMetadataMap.collectAsState()

    // Real-time Permission states rechecked on resume
    var isNotifAccessGranted by remember { mutableStateOf(ServiceControlHelper.isNotificationListenerGranted(context)) }
    var isBatteryOptIgnored by remember { mutableStateOf(ServiceControlHelper.isBatteryOptimizationIgnored(context)) }
    var isComponentEnabled by remember { mutableStateOf(ServiceControlHelper.isServiceComponentEnabled(context)) }

    fun recheckHealth() {
        isNotifAccessGranted = ServiceControlHelper.isNotificationListenerGranted(context)
        isBatteryOptIgnored = ServiceControlHelper.isBatteryOptimizationIgnored(context)
        isComponentEnabled = ServiceControlHelper.isServiceComponentEnabled(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                recheckHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Backup & Restore Launchers
    var restoreDialogResult by remember { mutableStateOf<BackupRestoreHelper.RestoreResult?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportFullBackup(it) { success ->
                Toast.makeText(
                    context,
                    if (success) strings.backupExportSuccess else "Export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importFullBackup(it) { result ->
                restoreDialogResult = result
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportCsv(it) { success ->
                Toast.makeText(
                    context,
                    if (success) strings.csvExportSuccess else "CSV export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // App Filter list states
    var appSearchQuery by remember { mutableStateOf("") }
    var selectedFilterMode by remember { mutableStateOf(0) } // 0 = All, 1 = Logging ON, 2 = Save Image ON

    val filteredApps = remember(appMetadataMap, appSearchQuery, selectedFilterMode) {
        appMetadataMap.values
            .filter { meta ->
                if (appSearchQuery.isNotBlank()) {
                    val q = appSearchQuery.trim()
                    meta.appName.contains(q, ignoreCase = true) || meta.packageName.contains(q, ignoreCase = true)
                } else true
            }
            .filter { meta ->
                when (selectedFilterMode) {
                    1 -> meta.isTrackingEnabled
                    2 -> meta.isSaveImageEnabled
                    else -> true
                }
            }
            .sortedBy { it.appName.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
    ) {
        // Top App Bar
        Surface(
            color = Color(0xFF16161E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF4E95FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.tabSettings,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isBackupRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF4E95FF),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: System Health Check Dashboard (3 Status Indicators)
            item {
                SystemHealthCard(
                    isNotifGranted = isNotifAccessGranted,
                    isBatteryIgnored = isBatteryOptIgnored,
                    isComponentEnabled = isComponentEnabled && isBackgroundEnabled && isNotifAccessGranted,
                    onGrantNotif = { ServiceControlHelper.openNotificationListenerSettings(context) },
                    onUnrestrictBattery = { ServiceControlHelper.requestIgnoreBatteryOptimizations(context) },
                    onRebindService = {
                        ServiceControlHelper.ensureServiceActive(context)
                        recheckHealth()
                    }
                )
            }

            // Section 2: General & Language Settings
            item {
                GeneralSettingsCard(
                    languageCode = languageCode,
                    isBackgroundEnabled = isBackgroundEnabled,
                    isSilentNotifEnabled = isSilentNotifEnabled,
                    onLanguageChange = { viewModel.setLanguage(it) },
                    onBackgroundToggle = { viewModel.toggleBackgroundMaster(it) },
                    onSilentNotifToggle = { viewModel.toggleSilentForegroundNotif(it) }
                )
            }

            // Section 3: Single-File Backup, Merge Restore & CSV Export
            item {
                BackupRestoreCard(
                    isProcessing = isBackupRunning,
                    onExportBackup = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        exportBackupLauncher.launch("SNL_Backup_$timestamp.json")
                    },
                    onImportBackup = {
                        importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    onExportCsv = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        exportCsvLauncher.launch("SNL_History_$timestamp.csv")
                    }
                )
            }

            // Section 4 Header: Per-App Filter & Image Saving Manager
            item {
                AppFilterHeaderCard(
                    totalApps = appMetadataMap.size,
                    isSyncing = isSyncingApps,
                    searchQuery = appSearchQuery,
                    selectedFilterMode = selectedFilterMode,
                    onSearchChange = { appSearchQuery = it },
                    onFilterChange = { selectedFilterMode = it },
                    onSyncClick = { viewModel.syncInstalledApps() }
                )
            }

            // Section 4 List: Individual App Rows
            items(filteredApps, key = { it.packageName }) { meta ->
                AppFilterRowCard(
                    meta = meta,
                    onToggleTracking = { viewModel.setAppTrackingEnabled(meta.packageName, it) },
                    onToggleSaveImage = { viewModel.setAppSaveImageEnabled(meta.packageName, it) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "SNL Demo • 100% Offline • Zero Internet Access",
                    fontSize = 11.sp,
                    color = Color(0xFF555566),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    // Merge Restore Result Dialog
    restoreDialogResult?.let { result ->
        AlertDialog(
            onDismissRequest = { restoreDialogResult = null },
            title = {
                Text(
                    text = strings.restoreDialogTitle,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        strings.restoreSummary,
                        result.totalInFile,
                        result.newlyMergedCount,
                        result.skippedDuplicateCount
                    ),
                    color = Color(0xFFE2E2EC),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { restoreDialogResult = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E95FF))
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E2C)
        )
    }
}

/**
 * Section 1: System Health Card with 3 Status Indicators
 */
@Composable
fun SystemHealthCard(
    isNotifGranted: Boolean,
    isBatteryIgnored: Boolean,
    isComponentEnabled: Boolean,
    onGrantNotif: () -> Unit,
    onUnrestrictBattery: () -> Unit,
    onRebindService: () -> Unit
) {
    val strings = LocalAppStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF242434), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = strings.systemHealthTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Light 1: Notification Access Permission
            HealthItemRow(
                title = strings.permNotifAccess,
                isOk = isNotifGranted,
                statusText = if (isNotifGranted) strings.statusGranted else strings.statusMissing,
                buttonText = if (!isNotifGranted) strings.btnGrant else null,
                onButtonClick = onGrantNotif
            )

            HorizontalDivider(color = Color(0xFF20202E))

            // Light 2: Battery Optimization
            HealthItemRow(
                title = strings.permBatteryOpt,
                isOk = isBatteryIgnored,
                statusText = if (isBatteryIgnored) strings.statusGranted else strings.statusMissing,
                buttonText = if (!isBatteryIgnored) strings.btnUnrestricted else null,
                onButtonClick = onUnrestrictBattery
            )

            HorizontalDivider(color = Color(0xFF20202E))

            // Light 3: Background Listener Component Status
            HealthItemRow(
                title = strings.serviceRunningState,
                isOk = isComponentEnabled,
                statusText = if (isComponentEnabled) strings.statusActive else strings.statusInactive,
                buttonText = if (!isComponentEnabled) strings.btnRebind else null,
                onButtonClick = onRebindService
            )
        }
    }
}

@Composable
fun HealthItemRow(
    title: String,
    isOk: Boolean,
    statusText: String,
    buttonText: String?,
    onButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (isOk) Color(0xFF00E676) else Color(0xFFFF5252), CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = statusText,
                fontSize = 11.sp,
                color = if (isOk) Color(0xFF00E676) else Color(0xFFFF8A80)
            )
        }

        if (buttonText != null) {
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28283C)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = buttonText, fontSize = 11.sp, color = Color(0xFF4E95FF))
            }
        }
    }
}

/**
 * Section 2: General & Language Settings Card
 */
@Composable
fun GeneralSettingsCard(
    languageCode: String,
    isBackgroundEnabled: Boolean,
    isSilentNotifEnabled: Boolean,
    onLanguageChange: (String) -> Unit,
    onBackgroundToggle: (Boolean) -> Unit,
    onSilentNotifToggle: (Boolean) -> Unit
) {
    val strings = LocalAppStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF242434), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Master Service Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.backgroundServiceToggle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Switch(
                    checked = isBackgroundEnabled,
                    onCheckedChange = onBackgroundToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4E95FF),
                        uncheckedThumbColor = Color(0xFFA4B0BE),
                        uncheckedTrackColor = Color(0xFF242434)
                    )
                )
            }

            HorizontalDivider(color = Color(0xFF20202E))

            // Silent Keep-Alive Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.silentKeepAliveToggle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = strings.silentKeepAliveDesc,
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E9F)
                    )
                }
                Switch(
                    checked = isSilentNotifEnabled,
                    onCheckedChange = onSilentNotifToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00D2D3),
                        uncheckedThumbColor = Color(0xFFA4B0BE),
                        uncheckedTrackColor = Color(0xFF242434)
                    )
                )
            }

            HorizontalDivider(color = Color(0xFF20202E))

            // Instant Language Switcher
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = Color(0xFF4E95FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.languageToggleTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = languageCode == "vi",
                        onClick = { onLanguageChange("vi") },
                        label = { Text("🇻🇳 Tiếng Việt", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A),
                            selectedLabelColor = Color(0xFF90CAF9),
                            containerColor = Color(0xFF20202E),
                            labelColor = Color(0xFFA4B0BE)
                        )
                    )

                    FilterChip(
                        selected = languageCode == "en",
                        onClick = { onLanguageChange("en") },
                        label = { Text("🇬🇧 English", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A),
                            selectedLabelColor = Color(0xFF90CAF9),
                            containerColor = Color(0xFF20202E),
                            labelColor = Color(0xFFA4B0BE)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Section 3: Single-File Backup, Merge Restore & CSV Export Card
 */
@Composable
fun BackupRestoreCard(
    isProcessing: Boolean,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportCsv: () -> Unit
) {
    val strings = LocalAppStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF242434), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Color(0xFF00D2D3),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = strings.backupRestoreTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF4E95FF),
                    trackColor = Color(0xFF242434)
                )
                Text(
                    text = strings.backupProcessing,
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E9F)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportBackup,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = strings.btnExportBackup, fontSize = 11.sp, color = Color.White, maxLines = 1)
                }

                Button(
                    onClick = onImportBackup,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF134E4A)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = strings.btnImportBackup, fontSize = 11.sp, color = Color.White, maxLines = 1)
                }
            }

            OutlinedButton(
                onClick = onExportCsv,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282838))
            ) {
                Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF90CAF9))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = strings.btnExportCsv, fontSize = 12.sp, color = Color(0xFF90CAF9))
            }
        }
    }
}

/**
 * Section 4 Header: Search, Filter & App Scan Header
 */
@Composable
fun AppFilterHeaderCard(
    totalApps: Int,
    isSyncing: Boolean,
    searchQuery: String,
    selectedFilterMode: Int,
    onSearchChange: (String) -> Unit,
    onFilterChange: (Int) -> Unit,
    onSyncClick: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.appFilterTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$totalApps ${strings.scanAppsSubtitle}",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E9F)
                )
            }

            Button(
                onClick = onSyncClick,
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF20202E)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF4E95FF), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF4E95FF))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = strings.scanInstalledApps, fontSize = 11.sp, color = Color(0xFF4E95FF))
            }
        }

        // App Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(text = strings.searchHint, color = Color(0xFF757588), fontSize = 12.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF757588)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = Color(0xFF757588))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF16161E),
                unfocusedContainerColor = Color(0xFF16161E),
                focusedBorderColor = Color(0xFF4E95FF),
                unfocusedBorderColor = Color(0xFF242434),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Filter Mode Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilterMode == 0,
                onClick = { onFilterChange(0) },
                label = { Text(strings.filterAllApps, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF1E3A8A),
                    selectedLabelColor = Color(0xFF90CAF9),
                    containerColor = Color(0xFF16161E),
                    labelColor = Color(0xFFA4B0BE)
                )
            )

            FilterChip(
                selected = selectedFilterMode == 1,
                onClick = { onFilterChange(1) },
                label = { Text(strings.filterLoggingOn, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF1E3A8A),
                    selectedLabelColor = Color(0xFF90CAF9),
                    containerColor = Color(0xFF16161E),
                    labelColor = Color(0xFFA4B0BE)
                )
            )

            FilterChip(
                selected = selectedFilterMode == 2,
                onClick = { onFilterChange(2) },
                label = { Text(strings.filterSaveImageOn, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF1E3A8A),
                    selectedLabelColor = Color(0xFF90CAF9),
                    containerColor = Color(0xFF16161E),
                    labelColor = Color(0xFFA4B0BE)
                )
            )
        }
    }
}

/**
 * Section 4: Individual App Filter Row Card
 */
@Composable
fun AppFilterRowCard(
    meta: AppMetadataEntity,
    onToggleTracking: (Boolean) -> Unit,
    onToggleSaveImage: (Boolean) -> Unit
) {
    val strings = LocalAppStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF242434), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocalFileImage(
                path = meta.appIconPath,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meta.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = meta.packageName,
                    fontSize = 10.sp,
                    color = Color(0xFF8E8E9F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 2 Clearly Labeled Switches
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch 1: Log Notif
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.allowLogToggle,
                        fontSize = 10.sp,
                        color = if (meta.isTrackingEnabled) Color(0xFF4E95FF) else Color(0xFF8E8E9F)
                    )
                    Switch(
                        checked = meta.isTrackingEnabled,
                        onCheckedChange = onToggleTracking,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4E95FF),
                            uncheckedThumbColor = Color(0xFFA4B0BE),
                            uncheckedTrackColor = Color(0xFF242434)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                // Switch 2: Save Image
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.allowSaveImageToggle,
                        fontSize = 10.sp,
                        color = if (meta.isSaveImageEnabled && meta.isTrackingEnabled) Color(0xFF00D2D3) else Color(0xFF8E8E9F)
                    )
                    Switch(
                        checked = meta.isSaveImageEnabled,
                        onCheckedChange = onToggleSaveImage,
                        enabled = meta.isTrackingEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00D2D3),
                            uncheckedThumbColor = Color(0xFFA4B0BE),
                            uncheckedTrackColor = Color(0xFF242434)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}

