package com.example.cvlab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cvlab.theme.*

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero section
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "CV AIM ANALYSIS LAB",
                color        = Mint,
                fontSize     = 11.sp,
                fontWeight   = FontWeight.Bold,
                letterSpacing = 3.sp,
            )
            Text(
                "Training & Vision Platform",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "A self-contained research tool for computer vision, object tracking, and aim analysis.\n" +
                "No connection to third-party games or external services.",
                color    = Color(0xFF8899AA),
                fontSize = 12.sp,
            )
        }

        // Feature cards grid
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(
                icon        = Icons.Default.GpsFixed,
                title       = "Shooting Simulator",
                description = "Moving targets, hitbox geometry, shot classification",
                accentColor = Coral,
                route       = "simulator",
                modifier    = Modifier.weight(1f),
                onNavigate  = onNavigate,
            )
            FeatureCard(
                icon        = Icons.Default.Science,
                title       = "CV Lab",
                description = "Import images, run detection pipeline, visualize boxes",
                accentColor = Violet,
                route       = "visionlab",
                modifier    = Modifier.weight(1f),
                onNavigate  = onNavigate,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(
                icon        = Icons.Default.TrackChanges,
                title       = "Tracking Lab",
                description = "Multi-object tracker with Kalman filter & history trails",
                accentColor = Sky,
                route       = "tracking",
                modifier    = Modifier.weight(1f),
                onNavigate  = onNavigate,
            )
            FeatureCard(
                icon        = Icons.Default.Analytics,
                title       = "Analytics",
                description = "Latency, accuracy, tracking error, system resources",
                accentColor = Gold,
                route       = "analytics",
                modifier    = Modifier.weight(1f),
                onNavigate  = onNavigate,
            )
        }

        // About section
        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape  = RoundedCornerShape(14.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ABOUT THIS APP", color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

                InfoRow(Icons.Default.Shield,    "No third-party game connections",  "Standalone educational simulator only")
                InfoRow(Icons.Default.PhoneAndroid, "On-device inference ready",    "TFLite interface (model not bundled)")
                InfoRow(Icons.Default.Tune,      "Geometry-based collision",         "All shot results from ground-truth hitboxes")
                InfoRow(Icons.Default.Visibility,"Transparent data labelling",       "Synthetic results clearly distinguished from real")
            }
        }

        // Technology stack badges
        Text("TECHNOLOGY STACK", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TechBadge("Kotlin",         Mint)
            TechBadge("Compose",        Sky)
            TechBadge("Material 3",     Violet)
            TechBadge("Coroutines",     Gold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TechBadge("TFLite",         Coral)
            TechBadge("Navigation",     Sky)
            TechBadge("Room",           Mint)
            TechBadge("Kalman Filter",  Gold)
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ---------------------------------------------------------------------------
// Components
// ---------------------------------------------------------------------------

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    route: String,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
) {
    Card(
        onClick  = { onNavigate(route) },
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = Panel),
        shape    = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            Text(title,       color = Color.White,   fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Color.LightGray, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Open", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accentColor, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = Mint, modifier = Modifier.size(16.dp))
        Column {
            Text(title,    color = Color.White,   fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.LightGray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TechBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
