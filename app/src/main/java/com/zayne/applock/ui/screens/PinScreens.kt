package com.zayne.applock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val PIN_LENGTH = 6

@Composable
fun PinKeypad(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(PIN_LENGTH) { index ->
                Surface(
                    shape = CircleShape,
                    color = if (index < value.length) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(16.dp)
                ) {}
            }
        }
        Spacer(Modifier.height(32.dp))
        val rows = listOf("123", "456", "789", " 0<")
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { c ->
                    when (c) {
                        ' ' -> Spacer(Modifier.size(64.dp))
                        '<' -> OutlinedButton(
                            onClick = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) },
                            modifier = Modifier.size(64.dp)
                        ) { Text("⌫") }
                        else -> Button(
                            onClick = {
                                if (value.length < PIN_LENGTH) {
                                    val newValue = value + c
                                    onValueChange(newValue)
                                    if (newValue.length == PIN_LENGTH) onSubmit(newValue)
                                }
                            },
                            modifier = Modifier.size(64.dp)
                        ) { Text(c.toString(), style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun PinSetupScreen(onPinConfirmed: (String) -> Unit) {
    var firstPin by remember { mutableStateOf<String?>(null) }
    var current by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (firstPin == null) "PIN festlegen (6 Ziffern)" else "PIN bestätigen",
            style = MaterialTheme.typography.titleMedium
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        PinKeypad(
            value = current,
            onValueChange = { current = it },
            onSubmit = { pin ->
                if (firstPin == null) {
                    firstPin = pin
                    current = ""
                    error = null
                } else if (pin == firstPin) {
                    onPinConfirmed(pin)
                } else {
                    error = "PINs stimmen nicht überein, bitte erneut."
                    firstPin = null
                    current = ""
                }
            }
        )
    }
}

/**
 * Kompakte PIN-Eingabe ohne eigenes Vollbild-Layout, zum Einbetten in eine Karte
 * (z.B. BiometricLockScreen).
 */
@Composable
fun PinEntryScreenContent(
    errorText: String?,
    onPinEntered: (String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PIN eingeben", style = MaterialTheme.typography.titleSmall)
        if (errorText != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorText, color = MaterialTheme.colorScheme.error)
        }
        PinKeypad(
            value = current,
            onValueChange = { current = it },
            onSubmit = { pin ->
                onPinEntered(pin)
                current = ""
            }
        )
    }
}
