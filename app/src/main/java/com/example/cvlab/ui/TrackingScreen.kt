package com.example.cvlab.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cvlab.theme.*
import com.example.cvlab.tracking.Track
import com.example.cvlab.tracking.TrackingViewModel

// Distinct track colours cycling through accent palette
private val TRACK_COLOURS = listOf(Mint, Gold, Coral, Sky, Violet, Color(0xFFFF9F43), Color(0xFF48DBB4), Color(0xFFFF6BA8))

@Composable
fun TrackingScreen(vm: TrackingViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TRACKING LAB", color = Sky, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Multi-Object Tracker", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = vm::onToggleKalman) {
                Icon(
                    if (state.useKalman) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                    contentDescription = "Toggle Kalman",
                    tint = if (state.useKalman) Mint else Color.Gray,
                )
            }
            IconButton(onClick = vm::onPauseResume) {
                Icon(
                    if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Pause/Resume",
                    tint = Gold,
                )
            }
            IconButton(onClick = vm::onReset) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Coral)
            }
        }

        // Metrics strip
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TrackMetric("TRACKS",  state.tracks.size.toString(),     Modifier.weight(1f), Mint)
            TrackMetric("FRAMES",  state.frameCount.toString(),      Modifier.weight(1f), Sky)
            TrackMetric("ERR(px)", if (state.meanError.isNaN()) "-" else String.format("%.1f", state.meanError), Modifier.weight(1f), Gold)
            TrackMetric("KALMAN",  if (state.useKalman) "ON" else "OFF", Modifier.weight(1f), Violet)
        }

        // Arena
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0B1624), RoundedCornerShape(14.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { vm.onArenaResized(it.width.toFloat(), it.height.toFloat()) }
            ) {
                val gridColor = Color(0xFF192A3E)
                var gx = 0f; while (gx <= size.width)  { drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), 1f); gx += 36f }
                var gy = 0f; while (gy <= size.height) { drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy),  1f); gy += 36f }

                // Draw ground-truth targets (dim)
                state.simulatorTargets.forEach { m ->
                    val c = Offset(m.target.center.x, m.target.center.y)
                    drawCircle(Color(0x3337D6A5), m.target.bodyRadius + 4f, c)
                    drawCircle(Color(0x5537D6A5), m.target.bodyRadius, c)
                }

                // Draw tracks with history trails
                state.tracks.forEachIndexed { i, track ->
                    val col = TRACK_COLOURS[track.id % TRACK_COLOURS.size]
                    val tc  = Offset(track.position.x, track.position.y)

                    // History trail
                    val hist = track.history
                    if (hist.size >= 2) {
                        for (j in 1 until hist.size) {
                            val alpha = j.toFloat() / hist.size
                            drawLine(
                                color    = col.copy(alpha = alpha * 0.6f),
                                start    = Offset(hist[j-1].x, hist[j-1].y),
                                end      = Offset(hist[j].x,   hist[j].y),
                                strokeWidth = 2f,
                                cap      = StrokeCap.Round,
                            )
                        }
                    }

                    // Track bounding box
                    drawRect(
                        color    = col.copy(alpha = 0.4f),
                        topLeft  = Offset(tc.x - 28f, tc.y - 40f),
                        size     = Size(56f, 72f),
                        style    = Stroke(1.5f),
                    )

                    // Track ID label area
                    drawRect(
                        color    = col.copy(alpha = 0.85f),
                        topLeft  = Offset(tc.x - 12f, tc.y - 54f),
                        size     = Size(24f, 14f),
                    )

                    // Track centre dot
                    drawCircle(col, 5f, tc)
                    drawCircle(Color.White, 2f, tc)
                }
            }

            // Track ID labels overlaid in Compose (Canvas drawText needs Paint on older APIs)
            state.tracks.forEach { track ->
                val col = TRACK_COLOURS[track.id % TRACK_COLOURS.size]
                val x   = track.position.x
                val y   = track.position.y
                if (x in 12f..(state.arenaWidth - 12f) && y in 54f..(state.arenaHeight - 12f)) {
                    Text(
                        "#${track.id}",
                        color      = Ink,
                        fontSize   = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier
                            .offset {
                                androidx.compose.ui.unit.IntOffset(
                                    (x - 10f).toInt(),
                                    (y - 54f).toInt()
                                )
                            }
                    )
                }
            }

            if (!state.isRunning) {
                Box(Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
                    Text("PAUSED", color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
                }
            }
        }

        // Note
        Text(
            "Synthetic targets + Gaussian noise feed the tracker. Ground truth (dim green) vs. track prediction (coloured) — error reported in pixels.",
            color    = Color(0xFF8899AA),
            fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )

        // Legend
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(Color(0xFF37D6A5), "Ground truth (GT)")
            LegendItem(Mint,              "Track prediction")
            LegendItem(Gold,              "Trail history")
        }
    }
}

@Composable
private fun TrackMetric(label: String, value: String, modifier: Modifier, valueColor: Color = Color.White) {
    Column(
        modifier = modifier.background(Panel, RoundedCornerShape(8.dp)).padding(vertical = 7.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, color = Color.LightGray, fontSize = 10.sp)
    }
}
