package com.example.cvlab.detection

import com.example.cvlab.geometry.BoundingBox
import com.example.cvlab.geometry.Vec2
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class TFLiteDetectorTest {

    @Test
    fun `detector reports not ready when initialized without model`() {
        val detector = TFLiteDetector(modelFileName = "")
        assertFalse(detector.isReady)
        assertTrue(detector.name.contains("UNAVAILABLE"))
        assertTrue(detector.statusMessage.contains("No TFLite model"))
    }

    @Test
    fun `detector gracefully handles missing asset file without crash`() {
        val detector = TFLiteDetector(modelFileName = "non_existent_model.tflite")
        assertFalse(detector.isReady)
        assertTrue(detector.name.contains("UNAVAILABLE"))
        assertTrue(detector.statusMessage.isNotEmpty())
    }

    @Test
    fun `detector gracefully handles corrupt byte buffer`() {
        val garbageBuffer = ByteBuffer.allocateDirect(64)
        garbageBuffer.put(ByteArray(64) { 0xFF.toByte() })
        garbageBuffer.rewind()

        val detector = TFLiteDetector(modelFileName = "corrupt.tflite", modelBuffer = garbageBuffer)
        assertFalse(detector.isReady)
        assertTrue(detector.statusMessage.contains("Failed to load model"))
    }

    @Test
    fun `detect returns empty list when detector is not ready`() = runBlocking {
        val detector = TFLiteDetector(modelFileName = "missing.tflite")
        val dummyPixels = IntArray(100 * 100) { 0xFF00FF00.toInt() }
        val results = detector.detect(dummyPixels, 100, 100, confidenceThreshold = 0.5f)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `synthetic detector produces detections marked with SYNTHETIC source`() = runBlocking {
        val detector = SyntheticDemoDetector()
        assertTrue(detector.isReady)
        val dummyPixels = IntArray(300 * 300) { 0xFF123456.toInt() }
        val results = detector.detect(dummyPixels, 300, 300, confidenceThreshold = 0.5f)
        assertTrue(results.isNotEmpty())
        for (det in results) {
            assertEquals(DetectionSource.SYNTHETIC, det.source)
            assertTrue(det.confidence >= 0.5f)
            assertTrue(det.box.width > 0f)
            assertTrue(det.box.height > 0f)
        }
    }

    @Test
    fun `synthetic detector respects confidence threshold filtering`() = runBlocking {
        val detector = SyntheticDemoDetector()
        val dummyPixels = IntArray(200 * 200) { 0 }
        val highThreshold = detector.detect(dummyPixels, 200, 200, confidenceThreshold = 0.99f)
        val lowThreshold = detector.detect(dummyPixels, 200, 200, confidenceThreshold = 0.1f)
        assertTrue(highThreshold.size <= lowThreshold.size)
    }

    @Test
    fun `inference timer records latency and computes statistics accurately`() {
        val timer = InferenceTimer()
        assertEquals(0, timer.stats().totalInferences)

        timer.record(10L)
        timer.record(20L)
        timer.record(30L)

        val stats = timer.stats()
        assertEquals(3, stats.totalInferences)
        assertEquals(30L, stats.lastLatencyMs)
        assertEquals(20.0, stats.avgLatencyMs, 0.01)
        assertEquals(20L, stats.p50LatencyMs)

        timer.reset()
        assertEquals(0, timer.stats().totalInferences)
    }
}
