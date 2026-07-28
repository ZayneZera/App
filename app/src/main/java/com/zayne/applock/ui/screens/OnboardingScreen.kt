package com.zayne.applock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class OnboardingStep(
    val title: String,
    val description: String,
    val done: Boolean,
    val actionLabel: String,
    val onAction: () -> Unit
)

@Composable
fun OnboardingScreen(
    steps: List<OnboardingStep>,
    allDone: Boolean,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Einrichtung", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Damit die Sperre zuverlässig funktioniert und wenig Akku verbraucht, " +
                "werden folgende Berechtigungen benötigt:",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

        steps.forEach { step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (step.done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (step.done) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(step.title, style = MaterialTheme.typography.titleSmall)
                    Text(step.description, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!step.done) {
                OutlinedButton(onClick = step.onAction, modifier = Modifier.fillMaxWidth()) {
                    Text(step.actionLabel)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            enabled = allDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Weiter")
        }
    }
}
