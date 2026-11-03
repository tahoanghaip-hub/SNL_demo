package com.local.snldemo.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.local.snldemo.data.local.AppDatabase
import com.local.snldemo.data.local.AppMetadataEntity
import com.local.snldemo.service.NotifLoggerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ServiceControlHelper {

    /**
     * Checks if notification listener access is granted for this package.
     */
    fun isNotificationListenerGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        if (enabledPackages.contains(context.packageName)) {
            return true
        }

        // Fallback: Check Settings.Secure enabled_notification_listeners string
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        if (!flat.isNullOrBlank()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == context.packageName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if the app is currently whitelisted from battery optimizations.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Checks if the NotifLoggerService component is enabled in the PackageManager.
     */
    fun isServiceComponentEnabled(context: Context): Boolean {
        val component = ComponentName(context, NotifLoggerService::class.java)
        val setting = context.packageManager.getComponentEnabledSetting(component)
        return setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }

    /**
     * Navigates the user directly to the system Notification Listener Access settings screen.
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Generic security settings fallback
            try {
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Requests battery optimization exemption or opens battery optimization settings.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val fallbackIntent =
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(fallbackIntent)
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Launches the target application by package name.
     * Returns true if the app was launched, false if not installed or cannot be opened.
     */
    fun openTargetApp(context: Context, packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Toggles the background notification logging service on or off.
     * Updates SharedPreferences, component state, and triggers service rebind or shutdown.
     */
    fun setBackgroundServiceEnabled(context: Context, enabled: Boolean) {
        val prefs = PreferenceManager.getInstance(context)
        prefs.isBackgroundEnabled = enabled

        val component = ComponentName(context, NotifLoggerService::class.java)
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            component,
            newState,
            PackageManager.DONT_KILL_APP
        )

        if (enabled) {
            if (isNotificationListenerGranted(context)) {
                ensureServiceActive(context)
            }
        } else {
            try {
                val stopIntent = Intent(context, NotifLoggerService::class.java).apply {
                    action = NotifLoggerService.ACTION_STOP_SERVICE
                }
                context.startService(stopIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Rebinds and keeps the NotifLoggerService alive across device reboots and strict OEM killers.
     */
    fun ensureServiceActive(context: Context) {
        val prefs = PreferenceManager.getInstance(context)
        if (!prefs.isBackgroundEnabled || !isNotificationListenerGranted(context)) {
            return
        }

        val component = ComponentName(context, NotifLoggerService::class.java)
        try {
            // Ensure component is enabled
            context.packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Rebind listener
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.service.notification.NotificationListenerService.requestRebind(component)
            }

            // Start foreground keep-alive service
            val serviceIntent = Intent(context, NotifLoggerService::class.java).apply {
                action = NotifLoggerService.ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Queries all installed applications, caches their app icons as WebP,
     * and seeds the Room app_metadata table for Whitelist/Blacklist filtering.
     * Strictly runs on Dispatchers.IO to maintain zero UI stutter.
     */
    suspend fun syncInstalledAppsToMetadata(context: Context) = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList()
        }

        val dao = AppDatabase.getInstance(context).notificationDao()
        val existingMetaList = dao.getAllAppMetadataSync()
        val existingPackages = existingMetaList.map { it.packageName }.toSet()

        for (appInfo in installedApps) {
            val pkg = appInfo.packageName
            // Skip SNL Demo itself
            if (pkg == context.packageName) continue

            // Include apps with a launcher intent or that already exist in database
            val hasLaunchIntent = pm.getLaunchIntentForPackage(pkg) != null
            if (hasLaunchIntent || existingPackages.contains(pkg)) {
                if (!existingPackages.contains(pkg)) {
                    val appName = try {
                        appInfo.loadLabel(pm).toString().ifBlank { pkg }
                    } catch (e: Exception) {
                        pkg
                    }

                    val iconDrawable = try {
                        appInfo.loadIcon(pm)
                    } catch (e: Exception) {
                        null
                    }

                    val iconPath = iconDrawable?.let {
                        StorageHelper.saveAppIconWebP(context, pkg, it)
                    }

                    val newMeta = AppMetadataEntity(
                        packageName = pkg,
                        appName = appName,
                        appIconPath = iconPath,
                        isTrackingEnabled = true,
                        isSaveImageEnabled = false
                    )
                    dao.upsertAppMetadata(newMeta)
                }
            }
        }
    }
}

