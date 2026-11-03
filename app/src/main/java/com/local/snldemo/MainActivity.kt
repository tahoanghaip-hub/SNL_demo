package com.local.snldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.snldemo.ui.MainViewModel
import com.local.snldemo.ui.i18n.AppLanguage
import com.local.snldemo.ui.i18n.LocalAppStrings
import com.local.snldemo.ui.i18n.getStrings
import com.local.snldemo.ui.screens.AppDetailScreen
import com.local.snldemo.ui.screens.HistoryHomeScreen
import com.local.snldemo.ui.theme.SNLDemoTheme
import com.local.snldemo.util.ServiceControlHelper

enum class AppTab {
    HISTORY,
    ANALYTICS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val languageCode by viewModel.languageCode.collectAsState()
            val currentLanguage = remember(languageCode) { AppLanguage.fromCode(languageCode) }
            val currentStrings = remember(currentLanguage) { getStrings(currentLanguage) }

            CompositionLocalProvider(LocalAppStrings provides currentStrings) {
                SNLDemoTheme {
                    MainAppShell(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically rebinds the offline listener if killed by aggressive OEM battery optimization
        ServiceControlHelper.ensureServiceActive(this)
    }
}

@Composable
fun MainAppShell(viewModel: MainViewModel) {
    val strings = LocalAppStrings.current
    var currentTab by remember { mutableStateOf(AppTab.HISTORY) }
    var selectedPackageDetail by remember { mutableStateOf<String?>(null) }

    // Intercept back button when inspecting App Detail screen
    if (selectedPackageDetail != null) {
        BackHandler {
            selectedPackageDetail = null
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F)),
        bottomBar = {
            // Keep bottom navigation visible on main screens (hide only in detail if desired)
            if (selectedPackageDetail == null) {
                NavigationBar(
                    containerColor = Color(0xFF16161E),
                    contentColor = Color(0xFF4E95FF),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.HISTORY,
                        onClick = { currentTab = AppTab.HISTORY },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = strings.tabHistory
                            )
                        },
                        label = {
                            Text(
                                text = strings.tabHistory,
                                fontSize = 11.sp,
                                fontWeight = if (currentTab == AppTab.HISTORY) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4E95FF),
                            selectedTextColor = Color(0xFF4E95FF),
                            indicatorColor = Color(0xFF1F2438),
                            unselectedIconColor = Color(0xFF8E8E9F),
                            unselectedTextColor = Color(0xFF8E8E9F)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.ANALYTICS,
                        onClick = { currentTab = AppTab.ANALYTICS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = strings.tabAnalytics
                            )
                        },
                        label = {
                            Text(
                                text = strings.tabAnalytics,
                                fontSize = 11.sp,
                                fontWeight = if (currentTab == AppTab.ANALYTICS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4E95FF),
                            selectedTextColor = Color(0xFF4E95FF),
                            indicatorColor = Color(0xFF1F2438),
                            unselectedIconColor = Color(0xFF8E8E9F),
                            unselectedTextColor = Color(0xFF8E8E9F)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.SETTINGS,
                        onClick = { currentTab = AppTab.SETTINGS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = strings.tabSettings
                            )
                        },
                        label = {
                            Text(
                                text = strings.tabSettings,
                                fontSize = 11.sp,
                                fontWeight = if (currentTab == AppTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4E95FF),
                            selectedTextColor = Color(0xFF4E95FF),
                            indicatorColor = Color(0xFF1F2438),
                            unselectedIconColor = Color(0xFF8E8E9F),
                            unselectedTextColor = Color(0xFF8E8E9F)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B0B0F))
        ) {
            when (currentTab) {
                AppTab.HISTORY -> {
                    if (selectedPackageDetail != null) {
                        AppDetailScreen(
                            packageName = selectedPackageDetail!!,
                            viewModel = viewModel,
                            onBack = { selectedPackageDetail = null }
                        )
                    } else {
                        HistoryHomeScreen(
                            viewModel = viewModel,
                            onAppClick = { pkg -> selectedPackageDetail = pkg },
                            onNavigateToSettings = { currentTab = AppTab.SETTINGS }
                        )
                    }
                }
                AppTab.ANALYTICS -> {
                    AnalyticsPlaceholderScreen()
                }
                AppTab.SETTINGS -> {
                    SettingsPlaceholderScreen(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Placeholder for Phase 4: Canvas Charts, PieChart, BarChart, and Storage Vacuum Cleaner.
 */
@Composable
fun AnalyticsPlaceholderScreen() {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF1E293B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.tabAnalytics,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Phase 4: Custom Canvas Pie Charts, Notification Heatmaps, and In-depth Storage Analytics",
            fontSize = 13.sp,
            color = Color(0xFF8E8E9F),
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Placeholder for Phase 5: System Health, App Whitelist/Blacklist, and JSON Backup/Restore.
 */
@Composable
fun SettingsPlaceholderScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val languageCode by viewModel.languageCode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = strings.tabSettings,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Language Switching Card (Zero-Restart Demonstration)
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
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = Color(0xFF4E95FF)
                    )
                    Text(
                        text = strings.languageToggleTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (languageCode == "vi") {
                        Button(
                            onClick = { viewModel.setLanguage("vi") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E95FF))
                        ) {
                            Text("Tiếng Việt (Active)")
                        }
                        OutlinedButton(
                            onClick = { viewModel.setLanguage("en") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("English")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.setLanguage("vi") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tiếng Việt")
                        }
                        Button(
                            onClick = { viewModel.setLanguage("en") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E95FF))
                        ) {
                            Text("English (Active)")
                        }
                    }
                }
            }
        }

        // Quick System Permissions
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00D2D3)
                    )
                    Text(
                        text = strings.systemHealthTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { ServiceControlHelper.openNotificationListenerSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222232))
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.permNotifAccess, color = Color.White)
                }

                Button(
                    onClick = { ServiceControlHelper.requestIgnoreBatteryOptimizations(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222232))
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.permBatteryOpt, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "SNL Demo • Phase 3 Complete • 100% Offline",
            fontSize = 11.sp,
            color = Color(0xFF555566),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
