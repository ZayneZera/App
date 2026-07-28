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
import androidx.compose.runtime.LaunchedEffect
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

/**
 * Blockiert den kompletten Task-Switcher (Recents), solange die zuletzt aktive App gesperrt
 * und nicht (mehr) authentifiziert ist - so ist auch das letzte sichtbare Vorschaubild
 * geschützt, ohne dass ein einzelnes Thumbnail manipuliert werden muss.
 */
class RecentsGuardActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AppLockApplication
        val targetPackage = LockStateTracker.lastForegroundPackage

        if (targetPackage == null || !LockStateTracker.lastForegroundNeedsAuth) {
            finish()
            return
        }

        val authMode = app.repository.authModeFor(targetPackage)

        setContent {
            AppLockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showPinFallback by remember {
                        mutableStateOf(authMode == AuthMode.PIN_ONLY || !BiometricAuthHelper.canUseBiometric(this))
                    }
                    var pinError by remember { mutableStateOf<String?>(null) }

                    if (!showPinFallback) {
                        LaunchedEffect(Unit) {
                            BiometricAuthHelper.authenticate(
                                activity = this@RecentsGuardActivity,
                                title = "Übersicht entsperren",
                                onSuccess = {
                                    LockStateTracker.markAuthenticated(targetPackage)
                                    finish()
                                },
                                onFailedOrError = { showPinFallback = true }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Übersicht gesperrt", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(24.dp))
                        if (showPinFallback) {
                            PinEntryScreen(title = "PIN eingeben", errorText = pinError) { pin ->
                                if (app.pinManager.verifyPin(pin)) {
                                    LockStateTracker.markAuthenticated(targetPackage)
                                    finish()
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

    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}
