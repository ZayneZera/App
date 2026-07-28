package com.zayne.applock.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.zayne.applock.AppLockApplication
import com.zayne.applock.data.AuthMode
import com.zayne.applock.security.BiometricAuthHelper
import com.zayne.applock.service.LockStateTracker
import com.zayne.applock.ui.screens.PinEntryScreen
import com.zayne.applock.ui.theme.AppLockTheme
import com.zayne.applock.util.AppInfoUtil

/**
 * Wird vom LockAccessibilityService über eine gesperrte App gelegt. Bleibt so lange offen,
 * bis Fingerabdruck oder PIN erfolgreich waren. Erscheint nicht im Task-Switcher.
 */
class LockOverlayActivity : FragmentActivity() {

    private var targetPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""

        val app = application as AppLockApplication
        val appLabel = runCatching {
            AppInfoUtil.listLaunchableApps(this).firstOrNull { it.packageName == targetPackage }?.label
        }.getOrNull() ?: targetPackage

        val authMode = app.repository.authModeFor(targetPackage)

        setContent {
            AppLockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showPinFallback by remember {
                        mutableStateOf(authMode == AuthMode.PIN_ONLY || !BiometricAuthHelper.canUseBiometric(this))
                    }
                    var pinError by remember { mutableStateOf<String?>(null) }

                    if (!showPinFallback) {
                        LaunchedEffectBiometric(appLabel) { success ->
                            if (success) {
                                onUnlocked()
                            } else {
                                showPinFallback = true
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("$appLabel ist gesperrt", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(24.dp))
                        if (showPinFallback) {
                            PinEntryScreen(
                                title = "PIN eingeben",
                                errorText = pinError
                            ) { pin ->
                                if (app.pinManager.verifyPin(pin)) {
                                    onUnlocked()
                                } else {
                                    pinError = "Falsche PIN"
                                }
                            }
                        } else {
                            Button(onClick = { showPinFallback = true }) {
                                Text("Stattdessen PIN verwenden")
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun LaunchedEffectBiometric(appLabel: String, onResult: (Boolean) -> Unit) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            BiometricAuthHelper.authenticate(
                activity = this@LockOverlayActivity,
                title = "$appLabel entsperren",
                onSuccess = { onResult(true) },
                onFailedOrError = { onResult(false) }
            )
        }
    }

    private fun onUnlocked() {
        LockStateTracker.markAuthenticated(targetPackage)
        finish()
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
