package com.local.snldemo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.local.snldemo.ui.AppStorageDetail
import com.local.snldemo.ui.MainViewModel
import com.local.snldemo.ui.components.LocalFileImage
import com.local.snldemo.ui.i18n.LocalAppStrings
import com.local.snldemo.util.StorageHelper

@Composable
fun AnalyticsStorageScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    // Auto-refresh metrics on-demand when entering screen
    LaunchedEffect(Unit) {
        viewModel.refreshAnalyticsAndStorage()
    }

    val storageState by viewModel.deviceStorageState.collectAsState()
    val appStorageDetails by viewModel.appStorageDetails.collectAsState()
    val last7DaysTraffic by viewModel.last7DaysTraffic.collectAsState()
    val isCalculating by viewModel.isCalculatingStorage.collectAsState()

    var activeCleanupApp by remember { mutableStateOf<AppStorageDetail?>(null) }

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
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = Color(0xFF4E95FF),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = strings.tabAnalytics,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                if (isCalculating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF4E95FF),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            viewModel.triggerManualVacuum {
                                Toast.makeText(context, strings.vacuumSuccessToast, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = strings.vacuumDbButton,
                            tint = Color(0xFF00D2D3),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refreshAnalyticsAndStorage() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFFA4B0BE),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Device Storage & App Footprint Card
            item {
                DeviceStorageCard(
                    storageState = storageState,
                    onVacuum = {
                        viewModel.triggerManualVacuum {
                            Toast.makeText(context, strings.vacuumSuccessToast, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Section 2: Interactive Donut / Pie Chart Card
            item {
                DonutChartCard(appStorageDetails = appStorageDetails)
            }

            // Section 3: Interactive Bar Chart Card
            item {
                BarChartCard(
                    last7Days = last7DaysTraffic,
                    topApps = appStorageDetails.take(6)
                )
            }

            // Section 4 Header: Storage by Application
            item {
                Text(
                    text = strings.storageByAppTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Section 4 List: Per-App Storage Details
            val maxBytes = appStorageDetails.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1L) ?: 1L
            items(appStorageDetails, key = { it.packageName }) { detail ->
                AppStorageRowCard(
                    detail = detail,
                    maxBytes = maxBytes,
                    onCleanClick = { activeCleanupApp = detail }
                )
            }
        }
    }

    // Flexible Cleanup Dialog
    activeCleanupApp?.let { app ->
        AppCleanupDialog(
            app = app,
            onDismiss = { activeCleanupApp = null },
            onCleanOlderThanDays = { days ->
                viewModel.cleanOlderThanDaysForApp(app.packageName, days) {
                    Toast.makeText(context, strings.vacuumSuccessToast, Toast.LENGTH_SHORT).show()
                    activeCleanupApp = null
                }
            },
            onCleanUnstarred = {
                viewModel.cleanUnstarredForApp(app.packageName) {
                    Toast.makeText(context, strings.vacuumSuccessToast, Toast.LENGTH_SHORT).show()
                    activeCleanupApp = null
                }
            },
            onCleanAll = {
                viewModel.cleanAllForApp(app.packageName) {
                    Toast.makeText(context, strings.vacuumSuccessToast, Toast.LENGTH_SHORT).show()
                    activeCleanupApp = null
                }
            }
        )
    }
}

/**
 * Section 1: Device Storage & App Footprint Card
 */
@Composable
fun DeviceStorageCard(
    storageState: com.local.snldemo.ui.DeviceStorageUiState,
    onVacuum: () -> Unit
) {
    val strings = LocalAppStrings.current

    val totalBytes = storageState.totalDeviceBytes
    val usedBytes = storageState.usedDeviceBytes
    val freeBytes = storageState.freeDeviceBytes
    val appBytes = storageState.totalAppOccupiedBytes

    val usedRatio = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

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
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color(0xFF00D2D3),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = strings.deviceStorageTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Visual multi-segment progress bar
            LinearProgressIndicator(
                progress = { usedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = Color(0xFF4E95FF),
                trackColor = Color(0xFF242434)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = strings.usedSpace, fontSize = 11.sp, color = Color(0xFF8E8E9F))
                    Text(
                        text = StorageHelper.formatBytes(usedBytes),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = strings.freeSpace, fontSize = 11.sp, color = Color(0xFF8E8E9F))
                    Text(
                        text = StorageHelper.formatBytes(freeBytes),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00E676)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = strings.totalSpace, fontSize = 11.sp, color = Color(0xFF8E8E9F))
                    Text(
                        text = StorageHelper.formatBytes(totalBytes),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF242434), thickness = 1.dp)

            // App Footprint Highlight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.appDbSize,
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E9F)
                    )
                    Text(
                        text = StorageHelper.formatBytes(appBytes),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D2D3)
                    )
                }

                Button(
                    onClick = onVacuum,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF20202E)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = Color(0xFF00D2D3),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.optimizeDbButton,
                        fontSize = 12.sp,
                        color = Color(0xFF00D2D3),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Slice Model for Custom Canvas Donut Chart
 */
data class ChartSlice(
    val label: String,
    val value: Long,
    val fraction: Float,
    val color: Color,
    val displayValue: String
)

/**
 * Section 2: Interactive Donut / Pie Chart Card
 */
@Composable
fun DonutChartCard(appStorageDetails: List<AppStorageDetail>) {
    val strings = LocalAppStrings.current
    var isBySizeMode by remember { mutableStateOf(false) } // Mode A = Count, Mode B = Size

    val palette = remember {
        listOf(
            Color(0xFF00E5FF),
            Color(0xFF7C4DFF),
            Color(0xFFFF4081),
            Color(0xFF00E676),
            Color(0xFFFFAB00),
            Color(0xFF2979FF),
            Color(0xFF9E9E9E)
        )
    }

    val slices = remember(appStorageDetails, isBySizeMode) {
        if (appStorageDetails.isEmpty()) emptyList()
        else {
            val total = if (isBySizeMode) {
                appStorageDetails.sumOf { it.totalBytes }.coerceAtLeast(1L)
            } else {
                appStorageDetails.sumOf { it.notificationCount.toLong() }.coerceAtLeast(1L)
            }

            val top6 = appStorageDetails.take(6)
            val others = appStorageDetails.drop(6)

            val result = mutableListOf<ChartSlice>()
            top6.forEachIndexed { index, app ->
                val v = if (isBySizeMode) app.totalBytes else app.notificationCount.toLong()
                val frac = v.toFloat() / total.toFloat()
                result.add(
                    ChartSlice(
                        label = app.appName,
                        value = v,
                        fraction = frac,
                        color = palette[index % palette.size],
                        displayValue = if (isBySizeMode) StorageHelper.formatBytes(v) else "$v"
                    )
                )
            }

            if (others.isNotEmpty()) {
                val othersVal = if (isBySizeMode) {
                    others.sumOf { it.totalBytes }
                } else {
                    others.sumOf { it.notificationCount.toLong() }
                }
                val frac = othersVal.toFloat() / total.toFloat()
                result.add(
                    ChartSlice(
                        label = strings.othersSlice,
                        value = othersVal,
                        fraction = frac,
                        color = palette.last(),
                        displayValue = if (isBySizeMode) StorageHelper.formatBytes(othersVal) else "$othersVal"
                    )
                )
            }
            result
        }
    }

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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.pieChartTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !isBySizeMode,
                        onClick = { isBySizeMode = false },
                        label = { Text(text = strings.chartByCount, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A),
                            selectedLabelColor = Color(0xFF90CAF9),
                            containerColor = Color(0xFF20202E),
                            labelColor = Color(0xFFA4B0BE)
                        )
                    )

                    FilterChip(
                        selected = isBySizeMode,
                        onClick = { isBySizeMode = true },
                        label = { Text(text = strings.chartBySize, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A),
                            selectedLabelColor = Color(0xFF90CAF9),
                            containerColor = Color(0xFF20202E),
                            labelColor = Color(0xFFA4B0BE)
                        )
                    )
                }
            }

            if (slices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = strings.noNotifications, color = Color(0xFF8E8E9F))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Custom Donut Canvas
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 24.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val arcSize = Size(diameter, diameter)
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                            var startAngle = -90f

                            slices.forEach { slice ->
                                val sweepAngle = slice.fraction * 360f
                                if (sweepAngle > 0f) {
                                    drawArc(
                                        color = slice.color,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                        }

                        // Center Metric
                        val totalDisplay = remember(slices, isBySizeMode) {
                            if (isBySizeMode) {
                                StorageHelper.formatBytes(slices.sumOf { it.value })
                            } else {
                                "${slices.sumOf { it.value }}"
                            }
                        }
                        Text(
                            text = totalDisplay,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Legend
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        slices.take(5).forEach { slice ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(slice.color, CircleShape)
                                )
                                Text(
                                    text = slice.label,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(slice.fraction * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA4B0BE)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 3: Interactive Bar Chart Card
 */
@Composable
fun BarChartCard(
    last7Days: List<com.local.snldemo.ui.DailyTrafficPoint>,
    topApps: List<AppStorageDetail>
) {
    val strings = LocalAppStrings.current
    var isTopAppsMode by remember { mutableStateOf(false) }

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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.barChartTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !isTopAppsMode,
                        onClick = { isTopAppsMode = false },
                        label = { Text(text = strings.chartLast7Days, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A),
                            selectedLabelColor = Color(0xFF90CAF9),
                            containerColor = Color(0xFF20202E),
                            labelColor = Color(0xFFA4B0BE)
                        )
                    )

                    FilterChip(
                        selected = isTopAppsMode,
                        onClick = { isTopAppsMode = true },
                        label = { Text(text = strings.chartTopApps, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A),
                            selectedLabelColor = Color(0xFF90CAF9),
                            containerColor = Color(0xFF20202E),
                            labelColor = Color(0xFFA4B0BE)
                        )
                    )
                }
            }

            // Custom Vertical Bar Chart Canvas
            val dataPoints = remember(last7Days, topApps, isTopAppsMode) {
                if (!isTopAppsMode) {
                    last7Days.map { Pair(it.dateLabel, it.count.toLong()) }
                } else {
                    topApps.map { Pair(it.appName.take(6), it.totalBytes) }
                }
            }

            if (dataPoints.isEmpty() || dataPoints.all { it.second == 0L }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = strings.noNotifications, color = Color(0xFF8E8E9F))
                }
            } else {
                val maxVal = dataPoints.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L

                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val count = dataPoints.size
                            val barWidth = 20.dp.toPx()
                            val spacing = (size.width - (count * barWidth)) / (count + 1)

                            dataPoints.forEachIndexed { i, pt ->
                                val x = spacing + i * (barWidth + spacing)
                                val barHeight = (pt.second.toFloat() / maxVal.toFloat()) * (size.height - 10.dp.toPx())
                                val y = size.height - barHeight

                                drawRoundRect(
                                    color = if (!isTopAppsMode) Color(0xFF4E95FF) else Color(0xFF00D2D3),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight.coerceAtLeast(6f)),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                        }
                    }

                    // X-Axis Labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dataPoints.forEach { pt ->
                            Text(
                                text = pt.first,
                                fontSize = 10.sp,
                                color = Color(0xFF8E8E9F),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 4: Per-App Storage Row Card
 */
@Composable
fun AppStorageRowCard(
    detail: AppStorageDetail,
    maxBytes: Long,
    onCleanClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val ratio = (detail.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF242434), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocalFileImage(
                    path = detail.iconPath,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = detail.packageName,
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E9F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = Color(0xFF20202E),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = StorageHelper.formatBytes(detail.totalBytes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4E95FF),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onCleanClick) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Cleanup",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Proportion bar
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF4E95FF),
                trackColor = Color(0xFF242434)
            )

            // Subtitle metrics breakdown (Clean notification count, recalled count, and total storage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${detail.notificationCount} TB" +
                            if (detail.removedCount > 0) " (${detail.removedCount} ${strings.badgeRemoved.lowercase()})" else "",
                    fontSize = 11.sp,
                    color = Color(0xFFA4B0BE)
                )

                Text(
                    text = "${strings.totalSpace}: ${StorageHelper.formatBytes(detail.totalBytes)}",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E9F)
                )
            }
        }
    }
}

/**
 * Flexible Per-App Cleanup Dialog
 */
@Composable
fun AppCleanupDialog(
    app: AppStorageDetail,
    onDismiss: () -> Unit,
    onCleanOlderThanDays: (Int) -> Unit,
    onCleanUnstarred: () -> Unit,
    onCleanAll: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedDays by remember { mutableIntStateOf(30) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF38384E), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = Color(0xFFFF5252)
                    )
                    Text(
                        text = "${strings.confirmCleanupTitle}: ${app.appName}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${strings.appDbSize}: ${StorageHelper.formatBytes(app.totalBytes)} (${app.notificationCount} notifications)",
                    fontSize = 12.sp,
                    color = Color(0xFFA4B0BE)
                )

                HorizontalDivider(color = Color(0xFF2E2E3E))

                // Option 1: Clean older than days
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.deleteOlderThanDays,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(7, 30, 90).forEach { days ->
                            FilterChip(
                                selected = selectedDays == days,
                                onClick = { selectedDays = days },
                                label = { Text("$days days", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1E3A8A),
                                    selectedLabelColor = Color(0xFF90CAF9),
                                    containerColor = Color(0xFF242434),
                                    labelColor = Color(0xFFA4B0BE)
                                )
                            )
                        }
                    }

                    Button(
                        onClick = { onCleanOlderThanDays(selectedDays) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28283C)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "${strings.actionDelete} (> $selectedDays days)", color = Color(0xFF90CAF9), fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFF2E2E3E))

                // Option 2: Clean unstarred only
                Button(
                    onClick = onCleanUnstarred,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28283C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = strings.deleteKeepStarred, color = Color(0xFFFFD700), fontSize = 12.sp)
                }

                // Option 3: Clean All for app (destructive)
                Button(
                    onClick = onCleanAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF741B1F)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.deleteAllForApp, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = strings.btnCancel, color = Color(0xFFA4B0BE))
                }
            }
        }
    }
}

