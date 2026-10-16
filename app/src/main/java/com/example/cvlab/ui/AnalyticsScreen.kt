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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cvlab.analytics.AnalyticsState
import com.example.cvlab.analytics.AnalyticsViewModel
import com.example.cvlab.theme.*

@Composable
fun AnalyticsScreen(vm: AnalyticsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Column {
            Text("ANALYTICS", color = Coral, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Performance Dashboard", style = MaterialTheme.typography.titleLarge)
            Text(
                "Measured values are clearly distinguished from synthetic demonstration data.",
                color = Color(0xFF8899AA), fontSize = 11.sp,
            )
        }

        // Synthetic data badge
        if (state.isSyntheticData) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x33FFD166)),
                shape  = RoundedCornerShape(10.dp),
            ) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                    Text(
                        "Simulator data is SYNTHETIC — values reflect ground-truth collision geometry, not real-world inference.",
                        color = Gold, fontSize = 10.sp,
                    )
                }
            }
        }

        // Shooting accuracy section
        SectionHeader("SHOOTING ACCURACY (Simulator)", Mint)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AnalyticsCard("SHOTS",   state.totalShots.toString(), Modifier.weight(1f))
            AnalyticsCard("HEAD HS", state.headHits.toString(),   Modifier.weight(1f), Mint)
            AnalyticsCard("BODY HS", state.bodyHits.toString(),   Modifier.weight(1f), Gold)
            AnalyticsCard("MISSES",  state.misses.toString(),     Modifier.weight(1f), Coral)
        }

        val accuracy = if (state.totalShots == 0) 0f else (state.headHits + state.bodyHits).toFloat() / state.totalShots
        val headRate = if (state.totalShots == 0) 0f else state.headHits.toFloat() / state.totalShots

        ProgressMetric("Overall accuracy", accuracy, Mint)
        ProgressMetric("Head-hit rate",    headRate,  Gold)

        // Vision inference section
        SectionHeader("VISION INFERENCE (Measured)", Sky)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AnalyticsCard("FRAMES",  state.framesProcessed.toString(), Modifier.weight(1f))
            AnalyticsCard("AVG ms",  if (state.avgLatencyMs == 0.0) "-" else String.format("%.1f", state.avgLatencyMs), Modifier.weight(1f), Sky)
            AnalyticsCard("p50 ms",  if (state.p50LatencyMs == 0L) "-" else "${state.p50LatencyMs}", Modifier.weight(1f), Sky)
            AnalyticsCard("p95 ms",  if (state.p95LatencyMs == 0L) "-" else "${state.p95LatencyMs}", Modifier.weight(1f), Violet)
        }

        // Tracking section
        SectionHeader("TRACKING (Synthetic Ground Truth)", Violet)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AnalyticsCard("TRACKS",  state.trackCount.toString(), Modifier.weight(1f), Violet)
            AnalyticsCard("ERR px",
                if (state.meanTrackError.isNaN()) "-" else String.format("%.1f", state.meanTrackError),
                Modifier.weight(1f), Gold)
        }

        // System section
        SectionHeader("SYSTEM RESOURCES", Color.LightGray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AnalyticsCard("MEM MB", state.memoryUsageMb.toString(), Modifier.weight(1f))
            AnalyticsCard("CPU %", "N/A*", Modifier.weight(1f), Color.Gray)
        }
        Text(
            "* CPU % requires PACKAGE_USAGE_STATS permission — not requested in this version.",
            color = Color(0xFF6677AA), fontSize = 9.sp,
        )

        // Unavailable metrics section
        SectionHeader("UNAVAILABLE METRICS", Color.Gray)
        UnavailableMetric("Detection precision/recall", "No labelled ground-truth dataset loaded.")
        UnavailableMetric("Bounding-box IoU (measured)", "Requires a real model and labelled images.")
        UnavailableMetric("Track continuity (measured)", "Synthetic tracker uses known ground truth; label needed for real data.")
        UnavailableMetric("Real-time screen-capture FPS", "MediaProjection not yet implemented — Phase 8 feature.")

        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Components
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(title, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun AnalyticsCard(label: String, value: String, modifier: Modifier, valueColor: Color = Color.White) {
    Column(
        modifier = modifier.background(Panel, RoundedCornerShape(10.dp)).padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor,      fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProgressMetric(label: String, fraction: Float, color: Color) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 12.sp)
            Text(String.format("%.0f%%", fraction * 100), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color    = color,
            trackColor = Border,
        )
    }
}

@Composable
private fun UnavailableMetric(label: String, reason: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Block, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
        Column {
            Text(label,  color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(reason, color = Color(0xFF556677), fontSize = 9.sp)
        }
    }
}
