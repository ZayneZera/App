package com.zayne.applock.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import com.zayne.applock.security.BiometricAuthHelper
import android.graphics.drawable.Drawable

private enum class GateMode { BIOMETRIC, PIN }

/**
 * Einheitlicher, cleaner Sperr-Screen: Fingerabdruck ist der einzige sichtbare Weg,
 * PIN ist nur ein unauffälliger Backup-Link. Kein Vollbild-Look, sondern eine schlichte
 * Karte am unteren Bildschirmrand - fühlt sich eher wie ein Overlay als wie eine neue App an.
 */
@Composable
fun BiometricLockScreen(
    activity: FragmentActivity,
    title: String,
    icon: Drawable?,
    pinOnly: Boolean,
    onVerifyPin: (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    var mode by remember {
        mutableStateOf(
            if (pinOnly || !BiometricAuthHelper.canUseBiometric(activity)) GateMode.PIN else GateMode.BIOMETRIC
        )
    }
    var statusText by remember { mutableStateOf("Mit Fingerabdruck entsperren") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    if (mode == GateMode.BIOMETRIC) {
        LaunchedEffect(attempt) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = title,
                onSuccess = onUnlocked,
                onError = { errorCode ->
                    if (errorCode == BiometricAuthHelper.ERROR_NEGATIVE_BUTTON) {
                        mode = GateMode.PIN
                    } else {
                        statusText = "Nicht erkannt – bitte erneut versuchen"
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                icon?.let { drawable ->
                    Image(
                        bitmap = drawable.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))

                AnimatedContent(targetState = mode, label = "gate-mode") { current ->
                    when (current) {
                        GateMode.BIOMETRIC -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(20.dp))
                            FilledIconButton(
                                onClick = { attempt++ },
                                modifier = Modifier.size(72.dp),
                                colors = IconButtonDefaults.filledIconButtonColors()
                            ) {
                                Icon(
                                    Icons.Filled.Fingerprint,
                                    contentDescription = "Fingerabdruck erneut versuchen",
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { mode = GateMode.PIN; pinError = null }) {
                                Text("PIN verwenden")
                            }
                        }

                        GateMode.PIN -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PinEntryScreenContent(errorText = pinError) { pin ->
                                if (onVerifyPin(pin)) {
                                    onUnlocked()
                                } else {
                                    pinError = "Falsche PIN"
                                }
                            }
                            if (!pinOnly && BiometricAuthHelper.canUseBiometric(activity)) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = {
                                    mode = GateMode.BIOMETRIC
                                    statusText = "Mit Fingerabdruck entsperren"
                                    attempt++
                                }) {
                                    Text("Fingerabdruck verwenden")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
