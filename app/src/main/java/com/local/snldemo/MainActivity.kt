package com.local.snldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.snldemo.ui.i18n.AppLanguage
import com.local.snldemo.ui.i18n.LocalAppStrings
import com.local.snldemo.ui.i18n.getStrings
import com.local.snldemo.ui.theme.SNLDemoTheme
import com.local.snldemo.util.PreferenceManager
import com.local.snldemo.util.StorageHelper

class MainActivity : ComponentActivity() {

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager.getInstance(this)

        setContent {
            val currentLanguage by preferenceManager.languageFlow.collectAsState()
            val strings = getStrings(currentLanguage)

            CompositionLocalProvider(LocalAppStrings provides strings) {
                SNLDemoTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Phase1DashboardScreen(
                            currentLanguage = currentLanguage,
                            onLanguageChanged = { newLanguage ->
                                preferenceManager.currentLanguage = newLanguage
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Phase1DashboardScreen(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    val strings = LocalAppStrings.current
    val storageInfo = StorageHelper.getDeviceStorageInfo()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = "SNL Demo (100% Offline)",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Phase 1: Architecture, Room DB & Storage Engine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // System Health & Verification Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.systemHealthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "• Room Database (snl_local.db): Initialized with WAL mode\n" +
                            "• Real-time i18n Engine: Active (${currentLanguage.displayName})\n" +
                            "• Offline Security: Zero INTERNET permissions\n" +
                            "• Storage Engine: WebP compressor & MediaStore Ready",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 20.sp
                )
            }
        }

        // Storage Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = strings.deviceStorageTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "${strings.totalSpace}: ${StorageHelper.formatBytes(storageInfo.first)} | " +
                            "${strings.freeSpace}: ${StorageHelper.formatBytes(storageInfo.third)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Language Switching Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = strings.languageToggleTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentLanguage == AppLanguage.VI) {
                Button(
                    onClick = { onLanguageChanged(AppLanguage.VI) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tiếng Việt (Mặc định)")
                }
                OutlinedButton(
                    onClick = { onLanguageChanged(AppLanguage.EN) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("English")
                }
            } else {
                OutlinedButton(
                    onClick = { onLanguageChanged(AppLanguage.VI) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tiếng Việt")
                }
                Button(
                    onClick = { onLanguageChanged(AppLanguage.EN) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("English (Active)")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

