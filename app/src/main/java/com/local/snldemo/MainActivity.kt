package com.local.snldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.snldemo.ui.MainViewModel
import com.local.snldemo.ui.i18n.AppLanguage
import com.local.snldemo.ui.i18n.LocalAppStrings
import com.local.snldemo.ui.i18n.getStrings
import com.local.snldemo.ui.screens.AnalyticsStorageScreen
import com.local.snldemo.ui.screens.AppDetailScreen
import com.local.snldemo.ui.screens.HistoryHomeScreen
import com.local.snldemo.ui.screens.SettingsScreen
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
                    AnalyticsStorageScreen(viewModel = viewModel)
                }
                AppTab.SETTINGS -> {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
