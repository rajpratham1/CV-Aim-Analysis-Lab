package com.example.cvlab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cvlab.theme.*

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cvlab.data.SettingsRepository

@Composable
fun SettingsScreen() {
    val currentSettings by SettingsRepository.settings.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text("SETTINGS", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Configuration", style = MaterialTheme.typography.titleLarge)
        }

        SettingsSection("Appearance") {
            ToggleRow(
                "Dark theme",
                currentSettings.darkTheme,
                { SettingsRepository.update { s -> s.copy(darkTheme = it) } },
                "UI colour scheme — toggle requires app restart"
            )
        }

        SettingsSection("Simulator") {
            ToggleRow(
                "Aim assist (simulator only)",
                currentSettings.aimAssistEnabled,
                { SettingsRepository.setAimAssist(it) },
                "Synthetic snap-to-target inside the simulator. Has no effect outside the app."
            )
        }

        SettingsSection("Tracking") {
            ToggleRow(
                "Kalman filter",
                currentSettings.kalmanEnabled,
                { SettingsRepository.setKalman(it) },
                "Enable 1-D constant-velocity Kalman prediction in the tracker"
            )
        }

        SettingsSection("Model Integration") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("TFLite model asset filename", color = Color.LightGray, fontSize = 12.sp)
                OutlinedTextField(
                    value         = currentSettings.modelName,
                    onValueChange = { SettingsRepository.setModelName(it) },
                    placeholder   = { Text("e.g. ssd_mobilenet_v1.tflite", fontSize = 12.sp) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Mint,
                        unfocusedBorderColor = Border,
                        cursorColor          = Mint,
                    ),
                )
                Text(
                    "Place the .tflite file in app/src/main/assets/ and enter its filename above. " +
                    "See docs/MODEL_INTEGRATION.md for expected tensor shapes.",
                    color    = Color(0xFF8899AA),
                    fontSize = 10.sp,
                )

                Text("Confidence threshold (%)", color = Color.LightGray, fontSize = 12.sp)
                OutlinedTextField(
                    value         = (currentSettings.confidenceThreshold * 100).toInt().toString(),
                    onValueChange = { input ->
                        val digits = input.filter { c -> c.isDigit() }
                        val pct = digits.toIntOrNull()?.coerceIn(1, 99) ?: 50
                        SettingsRepository.setConfidenceThreshold(pct / 100f)
                    },
                    modifier      = Modifier.width(100.dp),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Mint,
                        unfocusedBorderColor = Border,
                        cursorColor          = Mint,
                    ),
                )
            }
        }

        SettingsSection("Legal & Privacy") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LegalRow("This app does not connect to any external game, server, or online service.")
                LegalRow("No user data is sent off-device.")
                LegalRow("Image analysis is performed entirely on-device.")
                LegalRow("Aim assist operates only within this app's simulator.")
                LegalRow("See docs/PRIVACY.md for the full privacy policy.")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape  = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title.uppercase(), color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit, hint: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White,     fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(hint,  color = Color.LightGray, fontSize = 10.sp)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onChecked,
            colors          = SwitchDefaults.colors(checkedThumbColor = Mint, checkedTrackColor = MintDim),
        )
    }
}

@Composable
private fun LegalRow(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Mint, modifier = Modifier.size(13.dp).padding(top = 2.dp))
        Text(text, color = Color(0xFF9BAAB8), fontSize = 11.sp)
    }
}
