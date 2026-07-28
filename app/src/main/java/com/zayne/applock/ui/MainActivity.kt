package com.zayne.applock.ui

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.zayne.applock.AppLockApplication
import com.zayne.applock.admin.LockDeviceAdminReceiver
import com.zayne.applock.data.AuthMode
import com.zayne.applock.ui.screens.AppDetailScreen
import com.zayne.applock.ui.screens.AppListScreen
import com.zayne.applock.ui.screens.BiometricLockScreen
import com.zayne.applock.ui.screens.GlobalSettingsScreen
import com.zayne.applock.ui.screens.OnboardingScreen
import com.zayne.applock.ui.screens.OnboardingStep
import com.zayne.applock.ui.screens.PinSetupScreen
import com.zayne.applock.ui.theme.AppLockTheme
import com.zayne.applock.util.AppInfoUtil
import com.zayne.applock.util.InstalledAppInfo
import com.zayne.applock.util.PermissionChecks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed class Screen {
    data object Onboarding : Screen()
    data object PinSetup : Screen()
    data object AuthGate : Screen()
    data object AppList : Screen()
    data class AppDetail(val packageName: String) : Screen()
    data object GlobalSettings : Screen()
    data object ChangePin : Screen()
}

class MainActivity : FragmentActivity() {

    private var resumeTrigger by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumeTrigger++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AppLockApplication

        setContent {
            AppLockTheme {
                val context = LocalContext.current
                val configs by app.repository.configs.collectAsState()
                val settings by app.repository.settings.collectAsState()
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
                LaunchedEffect(Unit) {
                    apps = withContext(Dispatchers.IO) { AppInfoUtil.listLaunchableApps(context) }
                }

                var screen by remember {
                    mutableStateOf<Screen>(
                        if (!app.pinManager.isPinSet()) Screen.PinSetup else Screen.AuthGate
                    )
                }

                fun onboardingComplete(): Boolean =
                    PermissionChecks.isAccessibilityServiceEnabled(context) &&
                        PermissionChecks.isIgnoringBatteryOptimizations(context)

                when (val current = screen) {
                    is Screen.PinSetup -> {
                        PinSetupScreen { pin ->
                            app.pinManager.setPin(pin)
                            screen = Screen.Onboarding
                        }
                    }

                    is Screen.ChangePin -> {
                        PinSetupScreen { pin ->
                            app.pinManager.setPin(pin)
                            screen = Screen.GlobalSettings
                        }
                    }

                    is Screen.AuthGate -> {
                        BiometricLockScreen(
                            activity = this@MainActivity,
                            title = "App-Lock entsperren",
                            icon = null,
                            pinOnly = false,
                            onVerifyPin = { pin -> app.pinManager.verifyPin(pin) },
                            onUnlocked = {
                                screen = if (onboardingComplete()) Screen.AppList else Screen.Onboarding
                            }
                        )
                    }

                    is Screen.Onboarding -> {
                        resumeTrigger.let { /* erzwingt Recomposition bei App-Wechsel/Resume */ }
                        val accessibilityDone = PermissionChecks.isAccessibilityServiceEnabled(context)
                        val batteryDone = PermissionChecks.isIgnoringBatteryOptimizations(context)
                        OnboardingScreen(
                            steps = listOf(
                                OnboardingStep(
                                    title = "Bedienungshilfen-Dienst aktivieren",
                                    description = "Erkennt App-Wechsel, um die Sperre anzuzeigen. " +
                                        "Falls die Option ausgegraut ist: App-Info -> 3-Punkte-Menü -> " +
                                        "\"Eingeschränkte Einstellungen zulassen\".",
                                    done = accessibilityDone,
                                    actionLabel = "Bedienungshilfen öffnen",
                                    onAction = {
                                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    }
                                ),
                                OnboardingStep(
                                    title = "Akku-Optimierung deaktivieren",
                                    description = "Verhindert, dass Samsung den Hintergrunddienst beendet " +
                                        "und die Sperre dadurch unzuverlässig wird.",
                                    done = batteryDone,
                                    actionLabel = "Akku-Einstellungen öffnen",
                                    onAction = {
                                        startActivity(
                                            Intent(
                                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                Uri.parse("package:$packageName")
                                            )
                                        )
                                    }
                                )
                            ),
                            allDone = accessibilityDone && batteryDone,
                            onContinue = { screen = Screen.AppList }
                        )
                    }

                    is Screen.AppList -> {
                        var query by remember { mutableStateOf("") }
                        val lockedPackages = configs.filterValues { it.isLocked }.keys
                        AppListScreen(
                            apps = apps,
                            lockedPackages = lockedPackages,
                            searchQuery = query,
                            onSearchQueryChange = { query = it },
                            onToggleLock = { pkg, locked ->
                                coroutineScope.launch { app.repository.setLocked(pkg, locked) }
                            },
                            onOpenDetail = { pkg -> screen = Screen.AppDetail(pkg) },
                            onOpenSettings = { screen = Screen.GlobalSettings }
                        )
                    }

                    is Screen.AppDetail -> {
                        val pkg = current.packageName
                        val label = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg
                        val cfg = configs[pkg]
                        val useCustom = cfg?.customAuthMode != null || cfg?.customGraceMs != null
                        AppDetailScreen(
                            appLabel = label,
                            useCustomSettings = useCustom,
                            customAuthMode = cfg?.customAuthMode?.let {
                                runCatching { AuthMode.valueOf(it) }.getOrNull()
                            } ?: settings.defaultAuthMode,
                            customGraceMs = cfg?.customGraceMs ?: settings.defaultGraceMs,
                            onBack = { screen = Screen.AppList },
                            onSave = { useCustomSettings, authMode, graceMs ->
                                coroutineScope.launch {
                                    if (useCustomSettings) {
                                        app.repository.updateConfig(pkg, authMode, graceMs)
                                    } else {
                                        app.repository.updateConfig(pkg, null, null)
                                    }
                                }
                                screen = Screen.AppList
                            }
                        )
                    }

                    is Screen.GlobalSettings -> {
                        GlobalSettingsScreen(
                            defaultGraceMs = settings.defaultGraceMs,
                            defaultAuthMode = settings.defaultAuthMode,
                            recentsProtectionEnabled = settings.recentsProtectionEnabled,
                            deviceAdminActive = PermissionChecks.isDeviceAdminActive(context),
                            onBack = { screen = Screen.AppList },
                            onDefaultGraceChange = { ms ->
                                coroutineScope.launch { app.repository.setDefaultGraceMs(ms) }
                            },
                            onDefaultAuthModeChange = { mode ->
                                coroutineScope.launch { app.repository.setDefaultAuthMode(mode) }
                            },
                            onRecentsProtectionChange = { enabled ->
                                coroutineScope.launch { app.repository.setRecentsProtectionEnabled(enabled) }
                            },
                            onRequestDeviceAdmin = {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(
                                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                        LockDeviceAdminReceiver.componentName(context)
                                    )
                                    putExtra(
                                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                        "Verhindert die direkte Deinstallation der App."
                                    )
                                }
                                startActivity(intent)
                            },
                            onChangePin = { screen = Screen.ChangePin }
                        )
                    }
                }
            }
        }
    }
}

