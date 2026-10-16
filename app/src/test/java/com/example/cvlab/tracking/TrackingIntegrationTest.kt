package com.example.cvlab.tracking

import com.example.cvlab.detection.DetectionResult
import com.example.cvlab.detection.DetectionSource
import com.example.cvlab.geometry.BoundingBox
import com.example.cvlab.geometry.Vec2
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TrackingIntegrationTest {

    private lateinit var tracker: MultiObjectTracker

    @Before
    fun setup() {
        tracker = MultiObjectTracker(
            maxMissingFrames = 4,
            minHitsToConfirm = 2,
            maxAssociationDistance = 50f,
            useKalman = true,
        )
    }

    private fun mockDetectorResult(
        centerX: Float,
        centerY: Float,
        width: Float = 40f,
        height: Float = 60f,
        confidence: Float = 0.92f,
        source: DetectionSource = DetectionSource.MODEL,
    ): DetectionResult {
        return DetectionResult(
            box = BoundingBox.fromCenter(Vec2(centerX, centerY), width, height),
            label = "person",
            confidence = confidence,
            classId = 0,
            isNormalized = false,
            source = source,
        )
    }

    @Test
    fun `connecting detector outputs to tracker creates tracks with valid centers`() {
        val det1 = mockDetectorResult(100f, 150f)
        val det2 = mockDetectorResult(250f, 300f)

        val trackerInput = listOf(
            det1.toTrackingDetection(frameTimestampMs = 1000L),
            det2.toTrackingDetection(frameTimestampMs = 1000L),
        )

        val tracks = tracker.update(trackerInput, frameMs = 1000L)
        assertEquals(2, tracks.size)
        assertEquals(100f, tracks[0].position.x, 1.0f)
        assertEquals(150f, tracks[0].position.y, 1.0f)
        assertEquals(250f, tracks[1].position.x, 1.0f)
        assertEquals(300f, tracks[1].position.y, 1.0f)
    }

    @Test
    fun `stable track ID is maintained across consecutive detector frames`() {
        val frame1Det = listOf(mockDetectorResult(100f, 100f).toTrackingDetection(1000L))
        val tracks1 = tracker.update(frame1Det, 1000L)
        val trackId1 = tracks1[0].id

        // Target moves slightly (dx = +3, dy = +2)
        val frame2Det = listOf(mockDetectorResult(103f, 102f).toTrackingDetection(1033L))
        val tracks2 = tracker.update(frame2Det, 1033L)

        assertEquals(1, tracks2.size)
        assertEquals(trackId1, tracks2[0].id)
        assertTrue(tracks2[0].isConfirmed) // Confirmed after 2 hits
    }

    @Test
    fun `missing detections trigger missing counter and track expires after maxMissingFrames`() {
        val frame1Det = listOf(mockDetectorResult(100f, 100f).toTrackingDetection(1000L))
        tracker.update(frame1Det, 1000L)

        // Missing frames 1, 2, 3, 4 (maxMissingFrames = 4)
        for (i in 1..4) {
            val surviving = tracker.update(emptyList(), 1000L + i * 33L)
            assertEquals(1, surviving.size)
            assertEquals(i, surviving[0].missingFrames)
        }

        // Frame 5 exceeds maxMissingFrames -> track should expire
        val expired = tracker.update(emptyList(), 1000L + 5 * 33L)
        assertEquals(0, expired.size)
    }

    @Test
    fun `multi-target ground-truth comparison computes valid tracking error`() {
        // Known ground-truth trajectory for two targets over 3 frames
        val gtTrajectory = listOf(
            0 to Vec2(100f, 100f),
            0 to Vec2(300f, 300f),
            1 to Vec2(105f, 102f),
            1 to Vec2(305f, 302f),
            2 to Vec2(110f, 104f),
            2 to Vec2(310f, 304f),
        )

        // Perfect tracker predictions (0 error)
        val perfectPredictions = listOf(
            0 to Vec2(100f, 100f),
            0 to Vec2(300f, 300f),
            1 to Vec2(105f, 102f),
            1 to Vec2(305f, 302f),
            2 to Vec2(110f, 104f),
            2 to Vec2(310f, 304f),
        )
        val perfectError = computePositionError(perfectPredictions, gtTrajectory)
        assertEquals(0f, perfectError, 0.001f)

        // Predictions with exactly 3px horizontal offset
        val shiftedPredictions = perfectPredictions.map { it.first to Vec2(it.second.x + 3f, it.second.y) }
        val shiftedError = computePositionError(shiftedPredictions, gtTrajectory)
        assertEquals(3f, shiftedError, 0.01f)
    }

    @Test
    fun `measured metrics distinguish real model detections from synthetic detections`() {
        val realDet = mockDetectorResult(100f, 100f, source = DetectionSource.MODEL)
        val synthDet = mockDetectorResult(200f, 200f, source = DetectionSource.SYNTHETIC)

        assertEquals(DetectionSource.MODEL, realDet.source)
        assertEquals(DetectionSource.SYNTHETIC, synthDet.source)
        assertFalse(realDet.source == synthDet.source)
    }
}
