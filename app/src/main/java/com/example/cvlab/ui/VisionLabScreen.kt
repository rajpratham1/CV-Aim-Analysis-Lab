package com.example.cvlab.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cvlab.detection.DetectionResult
import com.example.cvlab.detection.DetectionSource
import com.example.cvlab.theme.*
import com.example.cvlab.vision.VisionViewModel

@Composable
fun VisionLabScreen(vm: VisionViewModel = viewModel()) {
    val state   = vm.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val scroll  = rememberScrollState()

    // Photo picker launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bitmap != null) vm.onImageSelected(uri, bitmap)
            } catch (e: Exception) {
                // surface error through state
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("COMPUTER VISION LAB", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Image Analysis", style = MaterialTheme.typography.titleLarge)
            }
            if (state.imageUri != null) {
                IconButton(onClick = vm::onClearImage) {
                    Icon(Icons.Default.Close, contentDescription = "Clear image", tint = Coral)
                }
            }
        }

        // Detector info badge
        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape  = RoundedCornerShape(10.dp),
        ) {
            Row(
                Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                Text(
                    "Detector: ${state.detectorName}",
                    color    = Color.LightGray,
                    fontSize = 11.sp,
                )
            }
        }

        // Image picker / preview
        if (state.originalBitmap == null) {
            // Empty picker card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(2.dp, Border, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Panel),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Sky, modifier = Modifier.size(48.dp))
                    Text("No image selected", color = Color.LightGray, fontSize = 14.sp)
                    Button(
                        onClick = { pickerLauncher.launch("image/*") },
                        colors  = ButtonDefaults.buttonColors(containerColor = Violet),
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import Image")
                    }
                }
            }
        } else {
            // Image with overlay bounding boxes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
            ) {
                Image(
                    bitmap         = state.originalBitmap.asImageBitmap(),
                    contentDescription = "Selected image",
                    contentScale   = ContentScale.Fit,
                    modifier       = Modifier.fillMaxSize(),
                )

                // Overlay bounding boxes using letterbox-aware coordinates
                if (state.detections.isNotEmpty()) {
                    val imgW = state.originalBitmap.width.toFloat()
                    val imgH = state.originalBitmap.height.toFloat()

                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        state.detections.forEach { det ->
                            val isSynthetic = det.source == DetectionSource.SYNTHETIC
                            val col = if (isSynthetic) Gold else Mint
                            val scaledBox = com.example.cvlab.geometry.scaleBox(
                                det.box, imgW, imgH, canvasW, canvasH, keepAspect = true
                            )

                            // Bounding box outline
                            drawRect(
                                color = col,
                                topLeft = Offset(scaledBox.left, scaledBox.top),
                                size = Size(scaledBox.width, scaledBox.height),
                                style = Stroke(width = 2.5f)
                            )

                            // Label background pill
                            val labelH = 16f
                            val labelW = (scaledBox.width * 0.8f).coerceIn(40f, 100f)
                            val labelTop = (scaledBox.top - labelH).coerceAtLeast(0f)
                            drawRect(
                                color = col.copy(alpha = 0.85f),
                                topLeft = Offset(scaledBox.left, labelTop),
                                size = Size(labelW, labelH)
                            )
                        }
                    }

                    // Compose labels positioned precisely above each bounding box
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val boxW = constraints.maxWidth.toFloat()
                        val boxH = constraints.maxHeight.toFloat()

                        state.detections.forEach { det ->
                            val scaledBox = com.example.cvlab.geometry.scaleBox(
                                det.box, imgW, imgH, boxW, boxH, keepAspect = true
                            )
                            val isSynthetic = det.source == DetectionSource.SYNTHETIC
                            val labelTag = if (isSynthetic) "[SYN]" else "[MODEL]"
                            val labelText = "$labelTag ${det.label} ${(det.confidence * 100).toInt()}%"
                            val labelTop = (scaledBox.top - 16f).coerceAtLeast(0f)

                            Text(
                                text = labelText,
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        scaledBox.left.toInt() + 2,
                                        labelTop.toInt()
                                    )
                                }
                            )
                        }
                    }
                }

                if (state.isProcessing) {
                    Box(Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Mint)
                    }
                }
            }

            // Confidence threshold
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Min confidence", color = Color.White, modifier = Modifier.width(110.dp), fontSize = 12.sp)
                Slider(
                    value         = state.confidenceThreshold,
                    onValueChange = vm::onThresholdChanged,
                    valueRange    = 0.1f..0.95f,
                    modifier      = Modifier.weight(1f),
                    colors        = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint),
                )
                Text(String.format("%.0f%%", state.confidenceThreshold * 100), color = Mint, modifier = Modifier.width(36.dp), fontSize = 12.sp)
            }

            // Run button
            Button(
                onClick  = vm::onRunDetection,
                enabled  = !state.isProcessing,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Violet),
                shape    = RoundedCornerShape(10.dp),
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("RUN DETECTION PIPELINE", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Error state
        state.errorMessage?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x33FF6B6B)),
                shape  = RoundedCornerShape(10.dp),
            ) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Coral)
                    Text(err, color = Coral, fontSize = 12.sp)
                }
            }
        }

        // Pipeline steps
        if (state.pipelineSteps.isNotEmpty()) {
            Text("PIPELINE STEPS", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            state.pipelineSteps.forEach { step ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (step.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint   = if (step.completed) Mint else Color.Gray,
                        modifier = Modifier.size(14.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(step.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(step.description, color = Color.LightGray, fontSize = 10.sp)
                    }
                    if (step.durationMs > 0) Text("${step.durationMs}ms", color = Sky, fontSize = 10.sp)
                }
            }
        }

        // Detections list
        if (state.detections.isNotEmpty()) {
            Text("DETECTIONS (${state.detections.size})", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)

            // Synthetic warning
            if (state.detections.any { it.source == DetectionSource.SYNTHETIC }) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x33FFD166)), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                        Text("These are SYNTHETIC demo detections — not real computer-vision predictions.", color = Gold, fontSize = 10.sp)
                    }
                }
            }

            state.detections.forEachIndexed { i, det ->
                DetectionRow(i, det)
            }

            // Inference stats
            val stats = state.inferenceStats
            if (stats.totalInferences > 0) {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("INFERENCE STATS", color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        StatsLine("Last latency", "${stats.lastLatencyMs}ms")
                        StatsLine("Average",      String.format("%.1fms", stats.avgLatencyMs))
                        StatsLine("p50",          "${stats.p50LatencyMs}ms")
                        StatsLine("p95",          "${stats.p95LatencyMs}ms")
                        StatsLine("Total runs",   stats.totalInferences.toString())
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetectionRow(index: Int, det: DetectionResult) {
    val color = if (det.source == DetectionSource.SYNTHETIC) Mint else Sky
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(Panel, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Column(Modifier.weight(1f)) {
            Text("${det.label} (cls ${det.classId})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Box: [${det.box.left.toInt()},${det.box.top.toInt()},${det.box.right.toInt()},${det.box.bottom.toInt()}]",
                color = Color.LightGray, fontSize = 10.sp,
            )
        }
        Text(
            String.format("%.0f%%", det.confidence * 100),
            color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        )
        if (det.source == DetectionSource.SYNTHETIC) {
            Text("[SYN]", color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatsLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.LightGray, fontSize = 11.sp)
        Text(value, color = Color.White,     fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
