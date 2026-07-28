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
 * Blockiert den kompletten Task-Switcher (Recents), solange die zuletzt aktive App gesperrt
 * und nicht (mehr) authentifiziert ist - so ist auch das letzte sichtbare Vorschaubild
 * geschützt, ohne dass ein einzelnes Thumbnail manipuliert werden muss.
 */
class RecentsGuardActivity : FragmentActivity() {

    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)

        val app = application as AppLockApplication
        val pkg = LockStateTracker.lastForegroundPackage
        targetPackage = pkg

        if (pkg == null || !LockStateTracker.lastForegroundNeedsAuth) {
            finish()
            return
        }

        val appInfo = runCatching {
            AppInfoUtil.listLaunchableApps(this).firstOrNull { it.packageName == pkg }
        }.getOrNull()
        val authMode = app.repository.authModeFor(pkg)

        setContent {
            AppLockTheme {
                BiometricLockScreen(
                    activity = this@RecentsGuardActivity,
                    title = "Übersicht entsperren",
                    icon = appInfo?.icon,
                    pinOnly = authMode == AuthMode.PIN_ONLY,
                    onVerifyPin = { pin -> app.pinManager.verifyPin(pin) },
                    onUnlocked = {
                        LockStateTracker.markAuthenticated(pkg)
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
        moveTaskToBack(true)
    }
}
