package com.local.snldemo.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.local.snldemo.util.PreferenceManager
import com.local.snldemo.util.ServiceControlHelper

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = PreferenceManager.getInstance(context)
            if (prefs.isBackgroundEnabled) {
                ServiceControlHelper.ensureServiceActive(context)
            }
        }
    }
}
