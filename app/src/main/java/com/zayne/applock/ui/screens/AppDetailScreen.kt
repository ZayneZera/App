package com.zayne.applock.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zayne.applock.data.AuthMode
import com.zayne.applock.util.GracePeriods

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appLabel: String,
    useCustomSettings: Boolean,
    customAuthMode: AuthMode,
    customGraceMs: Long,
    onBack: () -> Unit,
    onSave: (useCustom: Boolean, authMode: AuthMode, graceMs: Long) -> Unit
) {
    var custom by remember { mutableStateOf(useCustomSettings) }
    var authMode by remember { mutableStateOf(customAuthMode) }
    var graceMs by remember { mutableStateOf(customGraceMs) }
    var graceMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(appLabel) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Eigene Einstellungen für diese App", modifier = Modifier.weight(1f))
                Switch(checked = custom, onCheckedChange = { custom = it })
            }
            Spacer(Modifier.height(16.dp))

            if (custom) {
                Text("Entsperr-Methode", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = authMode == AuthMode.BIOMETRIC_THEN_PIN,
                        onClick = { authMode = AuthMode.BIOMETRIC_THEN_PIN }
                    )
                    Text("Fingerabdruck (mit PIN-Fallback)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = authMode == AuthMode.PIN_ONLY,
                        onClick = { authMode = AuthMode.PIN_ONLY }
                    )
                    Text("Nur PIN")
                }

                Spacer(Modifier.height(16.dp))
                Text("Erneut sperren nach Verlassen", style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = { graceMenuOpen = true }) {
                    Text(GracePeriods.labelFor(graceMs))
                }
                DropdownMenu(expanded = graceMenuOpen, onDismissRequest = { graceMenuOpen = false }) {
                    GracePeriods.options.forEach { (label, ms) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { graceMs = ms; graceMenuOpen = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(custom, authMode, graceMs) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Speichern") }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Zurück")
            }
        }
    }
}
