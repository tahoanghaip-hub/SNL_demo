package com.local.snldemo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.snldemo.data.local.AppSummaryProjection
import com.local.snldemo.data.local.NotificationEntity
import com.local.snldemo.ui.MainViewModel
import com.local.snldemo.ui.components.LocalFileImage
import com.local.snldemo.ui.components.NotificationCard
import com.local.snldemo.ui.i18n.LocalAppStrings
import com.local.snldemo.util.ServiceControlHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryHomeScreen(
    viewModel: MainViewModel,
    onAppClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    val isGranted = remember(context) {
        ServiceControlHelper.isNotificationListenerGranted(context)
    }

    val appSummaries by viewModel.appSummaries.collectAsState()
    val appMetadataMap by viewModel.appMetadataMap.collectAsState()
    val filteredNotifications by viewModel.filteredNotifications.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val startDateMillis by viewModel.startDateMillis.collectAsState()
    val endDateMillis by viewModel.endDateMillis.collectAsState()
    val onlyStarred by viewModel.onlyStarred.collectAsState()
    val onlyRemoved by viewModel.onlyRemoved.collectAsState()
    val isAnyFilterActive by viewModel.isAnyFilterActive.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
    ) {
        // 1. Permission Warning Banner if notification access is not granted
        if (!isGranted) {
            Surface(
                color = Color(0xFF381519),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.permNotifAccess,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.statusMissing,
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { ServiceControlHelper.openNotificationListenerSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Cấp quyền", fontSize = 12.sp, color = Color.White)
                        }
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282838)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = strings.tabSettings, fontSize = 12.sp, color = Color(0xFFA4B0BE))
                        }
                    }
                }
            }
        }

        // 2. Global Search & Filter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = strings.searchHint,
                        color = Color(0xFF757588),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF757588)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF757588)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
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

            // Horizontal Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Range Chip
                val dateLabel = if (startDateMillis != null && endDateMillis != null) {
                    val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                    "${sdf.format(Date(startDateMillis!!))} - ${sdf.format(Date(endDateMillis!!))}"
                } else {
                    strings.filterDateRange
                }

                FilterChip(
                    selected = startDateMillis != null,
                    onClick = { showDatePicker = true },
                    label = { Text(text = dateLabel, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (startDateMillis != null) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear date",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.setDateRange(null, null) }
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1E3A8A),
                        selectedLabelColor = Color(0xFF90CAF9),
                        containerColor = Color(0xFF16161E),
                        labelColor = Color(0xFFA4B0BE)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFF242434),
                        selectedBorderColor = Color(0xFF4E95FF),
                        enabled = true,
                        selected = startDateMillis != null
                    )
                )

                // Starred Chip
                FilterChip(
                    selected = onlyStarred,
                    onClick = { viewModel.toggleStarred() },
                    label = { Text(text = strings.filterStarred, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (onlyStarred) Color(0xFFFFD700) else Color(0xFFA4B0BE)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF332900),
                        selectedLabelColor = Color(0xFFFFD700),
                        containerColor = Color(0xFF16161E),
                        labelColor = Color(0xFFA4B0BE)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFF242434),
                        selectedBorderColor = Color(0xFFFFD700),
                        enabled = true,
                        selected = onlyStarred
                    )
                )

                // Removed Chip
                FilterChip(
                    selected = onlyRemoved,
                    onClick = { viewModel.toggleRemoved() },
                    label = { Text(text = strings.filterRemoved, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381519),
                        selectedLabelColor = Color(0xFFFF6B6B),
                        containerColor = Color(0xFF16161E),
                        labelColor = Color(0xFFA4B0BE)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFF242434),
                        selectedBorderColor = Color(0xFFFF5252),
                        enabled = true,
                        selected = onlyRemoved
                    )
                )

                // Clear Filters Chip
                if (isAnyFilterActive) {
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.clearAllFilters() },
                        label = { Text(text = strings.clearFilters, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF2A2A38),
                            labelColor = Color(0xFFE2E2EC)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFF38384E),
                            enabled = true,
                            selected = false
                        )
                    )
                }
            }
        }

        // 3. Dual View Mode
        if (!isAnyFilterActive) {
            // Mode A: Tracked Apps List
            if (appSummaries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF4E95FF),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = strings.noNotifications,
                            color = Color(0xFF8E8E9F),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(appSummaries, key = { it.packageName }) { summary ->
                        AppSummaryRowCard(
                            summary = summary,
                            iconPath = appMetadataMap[summary.packageName]?.appIconPath,
                            onClick = { onAppClick(summary.packageName) }
                        )
                    }
                }
            }
        } else {
            // Mode B: Active Multi-Filter Global Timeline
            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noNotifications,
                        color = Color(0xFF8E8E9F),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            showAppBadge = true,
                            onToggleStar = { viewModel.toggleStar(item) },
                            onOpenApp = { viewModel.openSourceApp(item.packageName) },
                            onDelete = { viewModel.deleteSingleNotification(item) }
                        )
                    }
                }
            }
        }
    }

    // Material 3 Date Range Picker Dialog
    if (showDatePicker) {
        SimpleDateRangeDialog(
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { start, end ->
                viewModel.setDateRange(start, end)
            }
        )
    }
}

@Composable
fun AppSummaryRowCard(
    summary: AppSummaryProjection,
    iconPath: String?,
    onClick: () -> Unit
) {
    val formattedLastTime = remember(summary.lastTimestamp) {
        if (summary.lastTimestamp > 0L) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(summary.lastTimestamp))
        } else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF242434), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocalFileImage(
                path = iconPath,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = summary.packageName,
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E9F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (formattedLastTime.isNotEmpty()) {
                    Text(
                        text = formattedLastTime,
                        fontSize = 10.sp,
                        color = Color(0xFF6B6B7E),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Total Count Badge
                Surface(
                    color = Color(0xFF20202E),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${summary.totalCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4E95FF),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Removed/Recalled Count Badge
                if (summary.removedCount > 0) {
                    Surface(
                        color = Color(0xFF381519),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Recalled: ${summary.removedCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF555566),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 2-Tab App Detail Screen: Tab 0 (Timeline) and Tab 1 (Grouped Conversations).
 */
@Composable
fun AppDetailScreen(
    packageName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current

    val appMetadataMap by viewModel.appMetadataMap.collectAsState()
    val allNotifications by viewModel.allNotifications.collectAsState()

    // Filter notifications belonging to this package
    val appNotifications = remember(allNotifications, packageName) {
        allNotifications.filter { it.packageName == packageName }
    }

    val meta = appMetadataMap[packageName]
    val appName = meta?.appName ?: packageName

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedSender by remember { mutableStateOf<String?>(null) }

    // Scoped filters
    var localSearchQuery by remember { mutableStateOf("") }
    var localOnlyStarred by remember { mutableStateOf(false) }
    var localOnlyRemoved by remember { mutableStateOf(false) }

    val filteredList = remember(
        appNotifications,
        localSearchQuery,
        localOnlyStarred,
        localOnlyRemoved,
        selectedSender,
        selectedTabIndex
    ) {
        appNotifications.filter { item ->
            if (selectedTabIndex == 1 && selectedSender != null && item.senderName != selectedSender) {
                return@filter false
            }
            if (localOnlyStarred && !item.isStarred) {
                return@filter false
            }
            if (localOnlyRemoved && !item.isRemoved) {
                return@filter false
            }
            if (localSearchQuery.isNotBlank()) {
                val q = localSearchQuery.trim()
                val match = item.title.contains(q, ignoreCase = true) ||
                        item.content.contains(q, ignoreCase = true) ||
                        item.senderName.contains(q, ignoreCase = true)
                if (!match) return@filter false
            }
            true
        }
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
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                LocalFileImage(
                    path = meta?.appIconPath,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = packageName,
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E9F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.openSourceApp(packageName) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = strings.actionOpenApp,
                        tint = Color(0xFF4E95FF)
                    )
                }
            }
        }

        // Tab Row: Tab 0 (Timeline) vs Tab 1 (Conversations)
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF16161E),
            contentColor = Color(0xFF4E95FF),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(0xFF4E95FF)
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Text(
                        text = strings.allTimelineTab,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTabIndex == 0) Color(0xFF4E95FF) else Color(0xFFA4B0BE)
                    )
                }
            )

            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Text(
                        text = strings.conversationsTab,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTabIndex == 1) Color(0xFF4E95FF) else Color(0xFFA4B0BE)
                    )
                }
            )
        }

        // Local Filter Bar: Row 1 = Full-Width Search Bar, Row 2 = Filter Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = localSearchQuery,
                onValueChange = { localSearchQuery = it },
                placeholder = { Text(text = strings.searchHint, color = Color(0xFF757588), fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF757588),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (localSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { localSearchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = Color(0xFF757588),
                                modifier = Modifier.size(16.dp)
                            )
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = localOnlyStarred,
                    onClick = { localOnlyStarred = !localOnlyStarred },
                    label = { Text("★ ${strings.actionStar}", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF332900),
                        selectedLabelColor = Color(0xFFFFD700),
                        containerColor = Color(0xFF16161E),
                        labelColor = Color(0xFFA4B0BE)
                    )
                )

                FilterChip(
                    selected = localOnlyRemoved,
                    onClick = { localOnlyRemoved = !localOnlyRemoved },
                    label = { Text(strings.badgeRemoved, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381519),
                        selectedLabelColor = Color(0xFFFF6B6B),
                        containerColor = Color(0xFF16161E),
                        labelColor = Color(0xFFA4B0BE)
                    )
                )

                if (localSearchQuery.isNotEmpty() || localOnlyStarred || localOnlyRemoved) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            localSearchQuery = ""
                            localOnlyStarred = false
                            localOnlyRemoved = false
                        },
                        label = { Text(strings.btnCancel, fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFFF5252)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF242434),
                            labelColor = Color(0xFFFF5252)
                        )
                    )
                }
            }
        }

        // Tab Content
        if (selectedTabIndex == 0) {
            // Tab 0: Chronological Timeline
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = strings.noNotifications, color = Color(0xFF8E8E9F))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            showAppBadge = false,
                            onToggleStar = { viewModel.toggleStar(item) },
                            onOpenApp = { viewModel.openSourceApp(item.packageName) },
                            onDelete = { viewModel.deleteSingleNotification(item) }
                        )
                    }
                }
            }
        } else {
            // Tab 1: Grouped Conversations
            if (selectedSender == null) {
                // Group by senderName
                val conversations = remember(filteredList) {
                    filteredList.groupBy { it.senderName }
                }

                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = strings.noConversations, color = Color(0xFF8E8E9F))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(conversations.keys.toList(), key = { it }) { sender ->
                            val messages = conversations[sender] ?: emptyList()
                            val latestMsg = messages.firstOrNull()
                            val recalledCount = messages.count { it.isRemoved }

                            ConversationSummaryCard(
                                senderName = sender,
                                avatarPath = latestMsg?.senderAvatarPath,
                                latestMessage = latestMsg?.content ?: "",
                                totalMessages = messages.size,
                                recalledCount = recalledCount,
                                latestTimestamp = latestMsg?.timestamp ?: 0L,
                                onClick = { selectedSender = sender }
                            )
                        }
                    }
                }
            } else {
                // Sender Specific Chat History
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = Color(0xFF1E1E2C),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { selectedSender = null }
                            ) {
                                Text(
                                    text = strings.backToConversations,
                                    color = Color(0xFF4E95FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = selectedSender ?: "",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            NotificationCard(
                                item = item,
                                showAppBadge = false,
                                onToggleStar = { viewModel.toggleStar(item) },
                                onOpenApp = { viewModel.openSourceApp(item.packageName) },
                                onDelete = { viewModel.deleteSingleNotification(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationSummaryCard(
    senderName: String,
    avatarPath: String?,
    latestMessage: String,
    totalMessages: Int,
    recalledCount: Int,
    latestTimestamp: Long,
    onClick: () -> Unit
) {
    val formattedTime = remember(latestTimestamp) {
        if (latestTimestamp > 0L) {
            SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date(latestTimestamp))
        } else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF242434), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocalFileImage(
                path = avatarPath,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (formattedTime.isNotEmpty()) {
                        Text(
                            text = formattedTime,
                            fontSize = 11.sp,
                            color = Color(0xFF8E8E9F)
                        )
                    }
                }

                Text(
                    text = latestMessage,
                    fontSize = 13.sp,
                    color = Color(0xFFA4B0BE),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFF20202E),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$totalMessages msg",
                            fontSize = 10.sp,
                            color = Color(0xFF4E95FF),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }

                    if (recalledCount > 0) {
                        Surface(
                            color = Color(0xFF381519),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Recalled: $recalledCount",
                                fontSize = 10.sp,
                                color = Color(0xFFFF6B6B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF555566),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDateRangeDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long?, Long?) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis?.let { it + 86399999L }
                    onDateRangeSelected(start, end)
                    onDismiss()
                }
            ) {
                Text("OK", color = Color(0xFF4E95FF), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA4B0BE))
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.weight(1f)
        )
    }
}

