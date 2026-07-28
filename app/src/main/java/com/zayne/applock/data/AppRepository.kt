package com.zayne.applock.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Hält den Room- und DataStore-Zustand zusätzlich als StateFlow im Speicher, damit der
 * AccessibilityService (der auf jedes Event schnell und synchron reagieren muss) nicht bei
 * jedem App-Wechsel auf die Datenbank zugreifen muss.
 */
class AppRepository(context: Context) {

    private val dao = AppLockDatabase.getInstance(context).appLockConfigDao()
    private val settingsStore = GlobalSettingsStore(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Fest eingebaute Sicherheits-Ausnahmen: eigener Launcher, Einstellungen, Standard-Telefon-App.
    // Werden nie gesperrt, damit man sich nicht versehentlich selbst aussperren kann.
    private val hardSafetyExceptions: Set<String> by lazy {
        buildSet {
            add("com.android.settings")
            runCatching {
                val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                    .addCategory(android.content.Intent.CATEGORY_HOME)
                context.packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName?.let { add(it) }
            }
            runCatching {
                val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                telecom?.defaultDialerPackage?.let { add(it) }
            }
        }
    }

    private val _configs = MutableStateFlow<Map<String, AppLockConfigEntity>>(emptyMap())
    val configs: StateFlow<Map<String, AppLockConfigEntity>> = _configs.asStateFlow()

    private val _settings = MutableStateFlow(GlobalSettings())
    val settings: StateFlow<GlobalSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            dao.observeAll().collect { list ->
                _configs.value = list.associateBy { it.packageName }
            }
        }
        scope.launch {
            settingsStore.settingsFlow.collect { _settings.value = it }
        }
    }

    fun configFor(packageName: String): AppLockConfigEntity? = _configs.value[packageName]

    fun isLocked(packageName: String): Boolean {
        if (packageName in hardSafetyExceptions) return false
        return configFor(packageName)?.isLocked == true
    }

    fun graceMsFor(packageName: String): Long =
        configFor(packageName)?.customGraceMs ?: _settings.value.defaultGraceMs

    fun authModeFor(packageName: String): AuthMode {
        val custom = configFor(packageName)?.customAuthMode
        return custom?.let { runCatching { AuthMode.valueOf(it) }.getOrNull() }
            ?: _settings.value.defaultAuthMode
    }

    suspend fun setLocked(packageName: String, locked: Boolean) {
        val existing = configFor(packageName) ?: AppLockConfigEntity(packageName = packageName)
        dao.upsert(existing.copy(isLocked = locked))
    }

    suspend fun updateConfig(
        packageName: String,
        customAuthMode: AuthMode?,
        customGraceMs: Long?
    ) {
        val existing = configFor(packageName) ?: AppLockConfigEntity(packageName = packageName)
        dao.upsert(
            existing.copy(
                customAuthMode = customAuthMode?.name,
                customGraceMs = customGraceMs
            )
        )
    }

    suspend fun setDefaultGraceMs(graceMs: Long) = settingsStore.setDefaultGraceMs(graceMs)
    suspend fun setDefaultAuthMode(mode: AuthMode) = settingsStore.setDefaultAuthMode(mode)
    suspend fun setRecentsProtectionEnabled(enabled: Boolean) =
        settingsStore.setRecentsProtectionEnabled(enabled)
    suspend fun setDeviceAdminRequested(requested: Boolean) =
        settingsStore.setDeviceAdminRequested(requested)
}
