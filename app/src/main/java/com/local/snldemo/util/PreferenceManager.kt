package com.local.snldemo.util

import android.content.Context
import android.content.SharedPreferences
import com.local.snldemo.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _languageFlow = MutableStateFlow(AppLanguage.fromCode(languageCode))
    val languageFlow: StateFlow<AppLanguage> = _languageFlow.asStateFlow()

    private val _backgroundEnabledFlow = MutableStateFlow(isBackgroundEnabled)
    val backgroundEnabledFlow: StateFlow<Boolean> = _backgroundEnabledFlow.asStateFlow()

    private val _foregroundSilentNotifEnabledFlow =
        MutableStateFlow(isForegroundSilentNotifEnabled)
    val foregroundSilentNotifEnabledFlow: StateFlow<Boolean> =
        _foregroundSilentNotifEnabledFlow.asStateFlow()

    var languageCode: String
        get() = prefs.getString(KEY_LANGUAGE_CODE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE_CODE, value).apply()
            _languageFlow.value = AppLanguage.fromCode(value)
        }

    var isBackgroundEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_BACKGROUND_ENABLED, value).apply()
            _backgroundEnabledFlow.value = value
        }

    var isForegroundSilentNotifEnabled: Boolean
        get() = prefs.getBoolean(KEY_FOREGROUND_SILENT_NOTIF_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FOREGROUND_SILENT_NOTIF_ENABLED, value).apply()
            _foregroundSilentNotifEnabledFlow.value = value
        }

    var currentLanguage: AppLanguage
        get() = AppLanguage.fromCode(languageCode)
        set(value) {
            languageCode = value.code
        }

    companion object {
        const val PREFS_NAME = "snl_prefs"
        const val KEY_LANGUAGE_CODE = "language_code"
        const val KEY_BACKGROUND_ENABLED = "is_background_enabled"
        const val KEY_FOREGROUND_SILENT_NOTIF_ENABLED = "is_foreground_silent_notif_enabled"
        const val DEFAULT_LANGUAGE = "vi"

        @Volatile
        private var INSTANCE: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

