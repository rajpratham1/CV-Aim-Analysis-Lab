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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cvlab.theme.*

@Composable
fun DocsScreen() {
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
            Text("DOCUMENTATION", color = Sky, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Architecture & Guides", style = MaterialTheme.typography.titleLarge)
        }

        DocSection("Project Overview") {
            DocText("CV Aim Analysis Lab is a self-contained Android research application demonstrating " +
                "real-time computer vision concepts, multi-object tracking, and collision-geometry-based " +
                "hit detection. It has no connection to third-party games or online services.")
        }

        DocSection("How modules differ") {
            DocBullet("Object detection", "A model (or synthetic demo) produces bounding boxes from image pixels.")
            DocBullet("Object tracking",  "Associates detections across frames using IoU distance and Kalman prediction.")
            DocBullet("Collision detection", "Tests whether a 2-D point lies inside a circular hitbox — purely geometric, no model involved.")
            DocBullet("Coordinate estimation", "Maps pixel coordinates through scaling and normalisation transforms.")
        }

        DocSection("Shooting Simulator") {
            DocText("Targets move via a simple bouncing-ball physics step. Each target has a body circle " +
                "and a head circle. Shot classification is based on these ground-truth circles — not on " +
                "a visual detector's guess. Aim assist is a snap-to-nearest feature that operates only " +
                "inside this screen.")
        }

        DocSection("Computer Vision Lab") {
            DocText("Import any image with the Android Photo Picker. The pipeline validates dimensions, " +
                "resizes to 320×320, extracts pixel data, then invokes the active Detector. Currently " +
                "only the synthetic demo detector is bundled. Place a SSD MobileNet v1 .tflite file in " +
                "assets/ and configure its name in Settings to enable real inference.")
        }

        DocSection("Model Integration") {
            DocText("Expected TFLite tensor format:")
            DocBullet("Input",   "[1, H, W, 3] float32, pixel values in [0, 1]")
            DocBullet("Output 0","[1, N, 4] — boxes [ymin, xmin, ymax, xmax] normalised")
            DocBullet("Output 1","[1, N] — class indices float32")
            DocBullet("Output 2","[1, N] — confidence scores float32")
            DocBullet("Output 3","[1] — valid detection count")
            DocText("See docs/MODEL_INTEGRATION.md for full instructions.")
        }

        DocSection("Tracker Architecture") {
            DocText("MultiObjectTracker uses greedy nearest-neighbour assignment (Euclidean distance). " +
                "Optional 1-D Kalman filters (constant velocity model) predict the next position before " +
                "matching each frame. Tracks expire after 5 consecutive missing frames. Track continuity " +
                "is computed against synthetic ground truth.")
        }

        DocSection("Privacy & Safety") {
            DocBullet("No data leaves the device",   "All processing is on-device.")
            DocBullet("No game interaction",          "No MediaProjection, overlay, or input injection.")
            DocBullet("No external model download",   "Models must be placed in assets/ manually.")
            DocBullet("Aim assist = simulator only",  "Cannot interact with other applications.")
        }

        DocSection("Known Limitations") {
            DocBullet("No pre-bundled .tflite model", "TFLiteDetector loads models from assets/. If none is provided, it reports unavailable state.")
            DocBullet("OpenCV not bundled",           "OpenCV requires an additional AAR. Add it to build.gradle.kts when needed.")
            DocBullet("CPU % unavailable",            "Requires PACKAGE_USAGE_STATS — not requested to minimise permissions.")
        }

        DocSection("Build & Run") {
            DocText("1. Open the project in Android Studio Electric Eel or newer.")
            DocText("2. Sync Gradle (File → Sync Project with Gradle Files).")
            DocText("3. Run on a device/emulator with minSdk 26+.")
            DocText("4. Unit tests: ./gradlew test")
            DocText("5. Instrumented tests: ./gradlew connectedAndroidTest")
            DocText("6. Debug APK: ./gradlew assembleDebug")
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DocSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape  = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title.uppercase(), color = Sky, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            content()
        }
    }
}

@Composable
private fun DocText(text: String) {
    Text(text, color = Color(0xFFBBCCDD), fontSize = 12.sp, lineHeight = 18.sp)
}

@Composable
private fun DocBullet(term: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("•", color = Mint, fontSize = 12.sp)
        Column {
            Text(term,   color = Color.White,      fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Color(0xFF8899AA), fontSize = 11.sp)
        }
    }
}
