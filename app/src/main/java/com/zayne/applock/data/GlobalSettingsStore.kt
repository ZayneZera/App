package com.zayne.applock.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "global_settings")

data class GlobalSettings(
    val defaultGraceMs: Long = 0L,
    val defaultAuthMode: AuthMode = AuthMode.BIOMETRIC_THEN_PIN,
    val recentsProtectionEnabled: Boolean = true,
    val deviceAdminRequested: Boolean = false
)

class GlobalSettingsStore(private val context: Context) {

    private object Keys {
        val GRACE_MS = longPreferencesKey("default_grace_ms")
        val AUTH_MODE = stringPreferencesKey("default_auth_mode")
        val RECENTS_PROTECTION = booleanPreferencesKey("recents_protection_enabled")
        val DEVICE_ADMIN_REQUESTED = booleanPreferencesKey("device_admin_requested")
    }

    val settingsFlow = context.dataStore.data.map { prefs ->
        GlobalSettings(
            defaultGraceMs = prefs[Keys.GRACE_MS] ?: 0L,
            defaultAuthMode = prefs[Keys.AUTH_MODE]?.let {
                runCatching { AuthMode.valueOf(it) }.getOrNull()
            } ?: AuthMode.BIOMETRIC_THEN_PIN,
            recentsProtectionEnabled = prefs[Keys.RECENTS_PROTECTION] ?: true,
            deviceAdminRequested = prefs[Keys.DEVICE_ADMIN_REQUESTED] ?: false
        )
    }

    suspend fun setDefaultGraceMs(graceMs: Long) {
        context.dataStore.edit { it[Keys.GRACE_MS] = graceMs }
    }

    suspend fun setDefaultAuthMode(mode: AuthMode) {
        context.dataStore.edit { it[Keys.AUTH_MODE] = mode.name }
    }

    suspend fun setRecentsProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RECENTS_PROTECTION] = enabled }
    }

    suspend fun setDeviceAdminRequested(requested: Boolean) {
        context.dataStore.edit { it[Keys.DEVICE_ADMIN_REQUESTED] = requested }
    }
}
