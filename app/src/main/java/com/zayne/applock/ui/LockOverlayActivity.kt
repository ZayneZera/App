package com.zayne.applock.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.zayne.applock.AppLockApplication
import com.zayne.applock.data.AuthMode
import com.zayne.applock.service.LockStateTracker
import com.zayne.applock.ui.screens.BiometricLockScreen
import com.zayne.applock.ui.theme.AppLockTheme
import com.zayne.applock.util.AppInfoUtil

/**
 * Wird vom LockAccessibilityService über eine gesperrte App gelegt. Bleibt so lange offen,
 * bis Fingerabdruck oder PIN erfolgreich waren. Erscheint nicht im Task-Switcher und ohne
 * Übergangs-Animation, damit es sich wie ein reines Overlay statt wie eine neue App anfühlt.
 */
class LockOverlayActivity : FragmentActivity() {

    private var targetPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""

        val app = application as AppLockApplication
        val appInfo = runCatching {
            AppInfoUtil.listLaunchableApps(this).firstOrNull { it.packageName == targetPackage }
        }.getOrNull()
        val appLabel = appInfo?.label ?: targetPackage
        val authMode = app.repository.authModeFor(targetPackage)

        setContent {
            AppLockTheme {
                BiometricLockScreen(
                    activity = this@LockOverlayActivity,
                    title = appLabel,
                    icon = appInfo?.icon,
                    pinOnly = authMode == AuthMode.PIN_ONLY,
                    onVerifyPin = { pin -> app.pinManager.verifyPin(pin) },
                    onUnlocked = {
                        LockStateTracker.markAuthenticated(targetPackage)
                        finishOverlay()
                    }
                )
            }
        }
    }

    private fun finishOverlay() {
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onBackPressed() {
        // Zurück-Taste schließt das Overlay nicht - die App bleibt gesperrt,
        // der Nutzer landet effektiv wieder auf dem Homescreen.
        moveTaskToBack(true)
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }
}
