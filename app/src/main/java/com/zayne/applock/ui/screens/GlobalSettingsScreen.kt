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
fun GlobalSettingsScreen(
    defaultGraceMs: Long,
    defaultAuthMode: AuthMode,
    recentsProtectionEnabled: Boolean,
    deviceAdminActive: Boolean,
    onBack: () -> Unit,
    onDefaultGraceChange: (Long) -> Unit,
    onDefaultAuthModeChange: (AuthMode) -> Unit,
    onRecentsProtectionChange: (Boolean) -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onChangePin: () -> Unit
) {
    var graceMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Einstellungen") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Standard-Entsperrmethode", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = defaultAuthMode == AuthMode.BIOMETRIC_THEN_PIN,
                    onClick = { onDefaultAuthModeChange(AuthMode.BIOMETRIC_THEN_PIN) }
                )
                Text("Fingerabdruck (mit PIN-Fallback)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = defaultAuthMode == AuthMode.PIN_ONLY,
                    onClick = { onDefaultAuthModeChange(AuthMode.PIN_ONLY) }
                )
                Text("Nur PIN")
            }

            Spacer(Modifier.height(16.dp))
            Text("Standard: erneut sperren nach Verlassen", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = { graceMenuOpen = true }) {
                Text(GracePeriods.labelFor(defaultGraceMs))
            }
            DropdownMenu(expanded = graceMenuOpen, onDismissRequest = { graceMenuOpen = false }) {
                GracePeriods.options.forEach { (label, ms) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onDefaultGraceChange(ms); graceMenuOpen = false }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Task-Manager-Schutz", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Versteckt den letzten Inhalt im Task-Switcher, wenn die zuletzt " +
                            "genutzte App gesperrt war.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = recentsProtectionEnabled, onCheckedChange = onRecentsProtectionChange)
            }

            Spacer(Modifier.height(16.dp))
            Text("Deinstallationsschutz", style = MaterialTheme.typography.titleSmall)
            Text(
                if (deviceAdminActive) "Aktiv" else "Nicht aktiv",
                style = MaterialTheme.typography.bodySmall
            )
            if (!deviceAdminActive) {
                OutlinedButton(onClick = onRequestDeviceAdmin, modifier = Modifier.fillMaxWidth()) {
                    Text("Als Geräteadministrator aktivieren")
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onChangePin, modifier = Modifier.fillMaxWidth()) {
                Text("PIN ändern")
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Zurück")
            }
        }
    }
}
