package com.example.cvlab.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cvlab.geometry.Vec2
import com.example.cvlab.simulator.*
import com.example.cvlab.theme.*

@Composable
fun SimulatorScreen(vm: SimulatorViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Header ---
        SimulatorHeader(
            isPaused     = state.isPaused,
            onPause      = vm::onPauseResume,
            onReset      = vm::onReset,
            onSettings   = { showSettings = !showSettings },
        )

        // --- Stats row ---
        StatsRow(state.stats)

        // --- Arena ---
        ArenaCanvas(
            state    = state,
            onMove   = vm::onCrosshairMoved,
            onResize = vm::onArenaResized,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        // --- Last result ---
        ResultBanner(state.lastResult)

        // --- Controls ---
        ControlRow(
            onFire  = vm::onFire,
            onReset = vm::onReset,
        )

        // --- Settings panel (collapsible) ---
        AnimatedVisibility(visible = showSettings, enter = fadeIn(), exit = fadeOut()) {
            SettingsPanel(state.settings, onSettings = vm::onSettingsChanged)
        }

        // --- Event log ---
        EventLog(state.events)
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun SimulatorHeader(
    isPaused: Boolean,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "SHOOTING SIMULATOR",
                color = Mint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                "Training Arena",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = onPause) {
            Icon(
                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Resume" else "Pause",
                tint = Mint,
            )
        }
        IconButton(onClick = onReset) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Sky)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Tune, contentDescription = "Settings", tint = Gold)
        }
    }
}

// ---------------------------------------------------------------------------
// Stats row
// ---------------------------------------------------------------------------

@Composable
private fun StatsRow(stats: SessionStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MetricCard("SHOTS",  stats.totalShots.toString(), Modifier.weight(1f))
        MetricCard("HEAD",   stats.headHits.toString(),   Modifier.weight(1f), Mint)
        MetricCard("BODY",   stats.bodyHits.toString(),   Modifier.weight(1f), Gold)
        MetricCard("MISS",   stats.misses.toString(),     Modifier.weight(1f), Coral)
        val pct = (stats.accuracy * 100).toInt()
        MetricCard("ACC%",  "$pct",                       Modifier.weight(1f), Sky)
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
) {
    Column(
        modifier = modifier
            .background(Panel, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------------------
// Arena canvas
// ---------------------------------------------------------------------------

@Composable
private fun ArenaCanvas(
    state: SimulatorUiState,
    onMove: (Vec2) -> Unit,
    onResize: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color(0xFF0C1624), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    onResize(size.width.toFloat(), size.height.toFloat())
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        onMove(
                            Vec2(
                                change.position.x.coerceIn(0f, size.width.toFloat()),
                                change.position.y.coerceIn(0f, size.height.toFloat()),
                            )
                        )
                    }
                }
        ) {
            val gridColor = Color(0xFF192A3E)
            // Grid lines
            var gx = 0f; while (gx <= size.width)  { drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), 1f); gx += 36f }
            var gy = 0f; while (gy <= size.height) { drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy),  1f); gy += 36f }

            // Targets
            state.targets.forEach { moving ->
                val t  = moving.target
                val bc = Offset(t.center.x, t.center.y)
                val hc = Offset(t.headCenter.x, t.headCenter.y)

                // Glow
                drawCircle(Color(0x2237FFB8), t.bodyRadius + 10f, bc)
                // Body
                drawCircle(Sky,          t.bodyRadius, bc)
                drawCircle(SkyDim,       t.bodyRadius, bc, style = Stroke(2f))
                // Head
                drawCircle(Gold,         t.headRadius, hc)
                drawCircle(GoldDim,      t.headRadius, hc, style = Stroke(2f))
                // Centre dot
                drawCircle(Color.White,  2.5f, hc)
            }

            // Crosshair
            val p = Offset(state.crosshair.x, state.crosshair.y)
            val cRed = Coral
            drawCircle(cRed, 14f, p, style = Stroke(2.5f))
            val gap = 6f; val len = 20f
            drawLine(cRed, Offset(p.x - len - gap, p.y), Offset(p.x - gap, p.y), 2.5f, StrokeCap.Round)
            drawLine(cRed, Offset(p.x + gap, p.y), Offset(p.x + len + gap, p.y), 2.5f, StrokeCap.Round)
            drawLine(cRed, Offset(p.x, p.y - len - gap), Offset(p.x, p.y - gap), 2.5f, StrokeCap.Round)
            drawLine(cRed, Offset(p.x, p.y + gap), Offset(p.x, p.y + len + gap), 2.5f, StrokeCap.Round)
            drawCircle(cRed, 2f, p)
        }

        // Overlay labels
        Text(
            "DRAG TO AIM",
            color     = Color(0x80FFFFFF),
            fontSize  = 9.sp,
            modifier  = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
        Text(
            "${state.targets.size} targets",
            color     = Mint,
            fontSize  = 10.sp,
            modifier  = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
        if (state.isPaused) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center,
            ) {
                Text("PAUSED", color = Gold, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
            }
        }
        if (state.settings.aimAssistEnabled) {
            Text(
                "AIM ASSIST ON (SYNTHETIC)",
                color    = Violet,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Result banner
// ---------------------------------------------------------------------------

@Composable
private fun ResultBanner(result: String) {
    val color = when {
        result.contains("HEAD") -> Mint
        result.contains("BODY") -> Gold
        result.contains("MISS") -> Coral
        else                    -> Color.White
    }
    Text(
        result,
        color      = color,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        modifier   = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    )
}

// ---------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------

@Composable
private fun ControlRow(onFire: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick  = onFire,
            modifier = Modifier.weight(2f).height(52.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White),
            shape    = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("FIRE", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        OutlinedButton(
            onClick  = onReset,
            modifier = Modifier.weight(1f).height(52.dp),
            shape    = RoundedCornerShape(12.dp),
        ) {
            Text("RESET", fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------------------------
// Settings panel
// ---------------------------------------------------------------------------

@Composable
private fun SettingsPanel(settings: SimulatorSettings, onSettings: (SimulatorSettings) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Panel),
        shape    = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SETTINGS", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)

            // Target count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Targets", color = Color.White, modifier = Modifier.width(72.dp), fontSize = 13.sp)
                Slider(
                    value         = settings.targetCount.toFloat(),
                    onValueChange = { onSettings(settings.copy(targetCount = it.toInt().coerceIn(1, 8))) },
                    valueRange    = 1f..8f,
                    steps         = 6,
                    modifier      = Modifier.weight(1f),
                    colors        = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint),
                )
                Text(settings.targetCount.toString(), color = Mint, modifier = Modifier.width(24.dp), fontSize = 13.sp)
            }

            // Speed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Speed", color = Color.White, modifier = Modifier.width(72.dp), fontSize = 13.sp)
                Slider(
                    value         = settings.speedMultiplier,
                    onValueChange = { onSettings(settings.copy(speedMultiplier = it)) },
                    valueRange    = 0.5f..4f,
                    modifier      = Modifier.weight(1f),
                    colors        = SliderDefaults.colors(thumbColor = Sky, activeTrackColor = Sky),
                )
                Text(String.format("%.1f", settings.speedMultiplier), color = Sky, modifier = Modifier.width(32.dp), fontSize = 12.sp)
            }

            // Difficulty
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Diff:", color = Color.White, fontSize = 13.sp)
                Difficulty.entries.forEach { d ->
                    val selected = settings.difficulty == d
                    val color    = if (selected) Mint else Color.LightGray
                    FilterChip(
                        selected = selected,
                        onClick  = { onSettings(settings.copy(difficulty = d)) },
                        label    = { Text(d.name, fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Mint,
                            selectedLabelColor     = Ink,
                        )
                    )
                }
            }

            // Aim assist
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Aim Assist (synthetic, simulator only)", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked         = settings.aimAssistEnabled,
                    onCheckedChange = { onSettings(settings.copy(aimAssistEnabled = it)) },
                    colors          = SwitchDefaults.colors(checkedThumbColor = Violet, checkedTrackColor = VioletDim),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Event log
// ---------------------------------------------------------------------------

@Composable
private fun EventLog(events: List<String>) {
    Text("RECENT EVENTS", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    LazyColumn(
        modifier                = Modifier.heightIn(max = 72.dp),
        verticalArrangement     = Arrangement.spacedBy(2.dp),
        reverseLayout           = false,
    ) {
        items(events) { event ->
            val color = when {
                event.contains("HEAD") -> Mint
                event.contains("BODY") -> Gold
                event.contains("MISS") -> Coral
                else                   -> Color(0xFFAABBCC)
            }
            Text("• $event", color = color, fontSize = 10.sp)
        }
    }
}
