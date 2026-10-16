package com.example.cvlab.tracking

import com.example.cvlab.geometry.Vec2
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TrackerTest {

    private lateinit var tracker: MultiObjectTracker

    @Before
    fun setup() {
        tracker = MultiObjectTracker(
            maxMissingFrames     = 3,
            minHitsToConfirm     = 2,
            maxAssociationDistance = 60f,
            useKalman            = false, // deterministic for tests
        )
    }

    private fun det(x: Float, y: Float, conf: Float = 0.9f) = Detection(
        boundingBoxCenter    = Vec2(x, y),
        width                = 40f,
        height               = 60f,
        confidence           = conf,
        frameTimestampMs     = System.currentTimeMillis(),
    )

    // -----------------------------------------------------------------------
    // Track creation and identity
    // -----------------------------------------------------------------------

    @Test
    fun `first frame creates one track per detection`() {
        val tracks = tracker.update(listOf(det(100f, 100f), det(300f, 300f)), 0L)
        assertEquals(2, tracks.size)
    }

    @Test
    fun `track ids are unique`() {
        tracker.update(listOf(det(100f, 100f), det(300f, 300f)), 0L)
        val tracks = tracker.update(listOf(det(102f, 102f), det(302f, 302f)), 16L)
        assertEquals(2, tracks.map { it.id }.toSet().size)
    }

    @Test
    fun `same track id is maintained across frames`() {
        val first  = tracker.update(listOf(det(100f, 100f)), 0L)
        val second = tracker.update(listOf(det(105f, 105f)), 16L) // slight movement
        assertEquals(first[0].id, second[0].id)
    }

    // -----------------------------------------------------------------------
    // Track confirmation
    // -----------------------------------------------------------------------

    @Test
    fun `track is unconfirmed after first hit`() {
        val tracks = tracker.update(listOf(det(100f, 100f)), 0L)
        assertFalse(tracks[0].isConfirmed)
    }

    @Test
    fun `track is confirmed after minHitsToConfirm frames`() {
        tracker.update(listOf(det(100f, 100f)), 0L)
        val tracks = tracker.update(listOf(det(103f, 103f)), 16L)
        assertTrue(tracks[0].isConfirmed) // minHits = 2
    }

    // -----------------------------------------------------------------------
    // Missing detections
    // -----------------------------------------------------------------------

    @Test
    fun `track survives one missing frame`() {
        tracker.update(listOf(det(100f, 100f)), 0L)
        val tracks = tracker.update(emptyList(), 16L) // missing frame
        assertEquals(1, tracks.size)
    }

    @Test
    fun `track expires after maxMissingFrames consecutive misses`() {
        tracker.update(listOf(det(100f, 100f)), 0L)
        repeat(4) { tracker.update(emptyList(), it.toLong() * 16) } // 4 > maxMissingFrames=3
        val tracks = tracker.currentTracks()
        assertEquals(0, tracks.size)
    }

    // -----------------------------------------------------------------------
    // History
    // -----------------------------------------------------------------------

    @Test
    fun `history grows with each matched frame`() {
        tracker.update(listOf(det(100f, 100f)), 0L)
        tracker.update(listOf(det(110f, 110f)), 16L)
        val tracks = tracker.update(listOf(det(120f, 120f)), 32L)
        assertTrue(tracks[0].history.size >= 3)
    }

    // -----------------------------------------------------------------------
    // Velocity estimation
    // -----------------------------------------------------------------------

    @Test
    fun `velocity direction matches movement`() {
        tracker.update(listOf(det(100f, 100f)), 0L)
        val tracks = tracker.update(listOf(det(110f, 100f)), 16L) // moving right
        assertTrue(tracks[0].velocity.x > 0f)
    }

    // -----------------------------------------------------------------------
    // Reset
    // -----------------------------------------------------------------------

    @Test
    fun `reset clears all tracks`() {
        tracker.update(listOf(det(100f, 100f), det(200f, 200f)), 0L)
        tracker.reset()
        assertEquals(0, tracker.currentTracks().size)
    }

    @Test
    fun `track ids restart from 1 after reset`() {
        tracker.update(listOf(det(100f, 100f)), 0L)
        tracker.reset()
        val tracks = tracker.update(listOf(det(100f, 100f)), 100L)
        assertEquals(1, tracks[0].id)
    }

    // -----------------------------------------------------------------------
    // Position error utility
    // -----------------------------------------------------------------------

    @Test
    fun `position error is 0 when predicted equals gt`() {
        val predicted   = listOf(0 to Vec2(100f, 100f), 1 to Vec2(200f, 200f))
        val groundTruth = listOf(0 to Vec2(100f, 100f), 1 to Vec2(200f, 200f))
        assertEquals(0f, computePositionError(predicted, groundTruth), 0.001f)
    }

    @Test
    fun `position error is NaN on empty input`() {
        assertTrue(computePositionError(emptyList(), emptyList()).isNaN())
    }

    @Test
    fun `position error matches expected pixel distance`() {
        val predicted   = listOf(0 to Vec2(0f, 0f))
        val groundTruth = listOf(0 to Vec2(3f, 4f)) // distance = 5
        assertEquals(5f, computePositionError(predicted, groundTruth), 0.01f)
    }
}
