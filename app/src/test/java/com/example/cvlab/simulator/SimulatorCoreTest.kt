package com.example.cvlab.simulator

import com.example.cvlab.geometry.Vec2
import org.junit.Assert.*
import org.junit.Test

class SimulatorCoreTest {

    private val centre = Vec2(100f, 100f)
    private val singleTarget = listOf(
        SimTarget(id = 1, center = centre, bodyRadius = 30f, headRadius = 15f)
    )

    // -----------------------------------------------------------------------
    // Shot classification: single target
    // -----------------------------------------------------------------------

    @Test
    fun `head hit when shot lands on head circle`() {
        // Head centre = (100, 100 - 30*0.75) = (100, 77.5)
        val (result, id) = SimulatorCore.classifyShot(Vec2(100f, 77.5f), singleTarget)
        assertEquals(ShotResult.HEAD_HIT, result)
        assertEquals(1, id)
    }

    @Test
    fun `head hit when shot is inside head radius`() {
        val headCenter = Vec2(100f, 77.5f)
        val point      = Vec2(headCenter.x + 10f, headCenter.y) // 10 < headRadius=15
        val (result, _) = SimulatorCore.classifyShot(point, singleTarget)
        assertEquals(ShotResult.HEAD_HIT, result)
    }

    @Test
    fun `body hit when shot lands on body circle`() {
        // Clearly inside body but outside head
        val (result, id) = SimulatorCore.classifyShot(Vec2(120f, 110f), singleTarget)
        assertEquals(ShotResult.BODY_HIT, result)
        assertEquals(1, id)
    }

    @Test
    fun `miss when shot lands far away`() {
        val (result, id) = SimulatorCore.classifyShot(Vec2(500f, 500f), singleTarget)
        assertEquals(ShotResult.MISS, result)
        assertNull(id)
    }

    @Test
    fun `head takes priority over body when both overlap`() {
        // Shoot exactly at head centre — must be HEAD, not BODY
        val headCenter = Vec2(centre.x, centre.y - 30f * 0.75f)
        val (result, _) = SimulatorCore.classifyShot(headCenter, singleTarget)
        assertEquals(ShotResult.HEAD_HIT, result)
    }

    @Test
    fun `shot just outside body radius is a miss`() {
        val outside = Vec2(centre.x + 31f, centre.y) // bodyRadius = 30
        val (result, _) = SimulatorCore.classifyShot(outside, singleTarget)
        assertEquals(ShotResult.MISS, result)
    }

    @Test
    fun `shot at body boundary is body hit`() {
        val boundary = Vec2(centre.x + 30f, centre.y) // exactly on edge
        val (result, _) = SimulatorCore.classifyShot(boundary, singleTarget)
        assertEquals(ShotResult.BODY_HIT, result)
    }

    // -----------------------------------------------------------------------
    // Multiple targets
    // -----------------------------------------------------------------------

    @Test
    fun `correct target id is returned for head hit`() {
        val targets = listOf(
            SimTarget(id = 10, center = Vec2(50f, 50f), bodyRadius = 20f, headRadius = 10f),
            SimTarget(id = 20, center = Vec2(200f, 200f), bodyRadius = 20f, headRadius = 10f),
        )
        // Shoot at head of target 20: (200, 200 - 20*0.75) = (200, 185)
        val (result, id) = SimulatorCore.classifyShot(Vec2(200f, 185f), targets)
        assertEquals(ShotResult.HEAD_HIT, result)
        assertEquals(20, id)
    }

    @Test
    fun `miss when target list is empty`() {
        val (result, id) = SimulatorCore.classifyShot(Vec2(100f, 100f), emptyList())
        assertEquals(ShotResult.MISS, result)
        assertNull(id)
    }

    @Test
    fun `shot in gap between two targets is a miss`() {
        val targets = listOf(
            SimTarget(id = 1, center = Vec2(0f, 0f),   bodyRadius = 20f, headRadius = 10f),
            SimTarget(id = 2, center = Vec2(100f, 0f), bodyRadius = 20f, headRadius = 10f),
        )
        val (result, _) = SimulatorCore.classifyShot(Vec2(50f, 0f), targets)
        assertEquals(ShotResult.MISS, result)
    }

    // -----------------------------------------------------------------------
    // Dead targets are not hit
    // -----------------------------------------------------------------------

    @Test
    fun `dead target does not count as a hit`() {
        val dead = listOf(SimTarget(id = 1, center = Vec2(100f, 100f), isAlive = false))
        val (result, _) = SimulatorCore.classifyShot(Vec2(100f, 100f), dead)
        assertEquals(ShotResult.MISS, result)
    }

    // -----------------------------------------------------------------------
    // Target movement / physics
    // -----------------------------------------------------------------------

    @Test
    fun `target moves in expected direction`() {
        val moving = MovingTarget(SimTarget(1, Vec2(100f, 100f)), vx = 5f, vy = 3f)
        val advanced = SimulatorCore.advanceTarget(moving, 800f, 600f)
        assertEquals(105f, advanced.target.center.x, 0.5f)
        assertEquals(103f, advanced.target.center.y, 0.5f)
    }

    @Test
    fun `target reflects off right wall`() {
        val moving = MovingTarget(SimTarget(1, Vec2(790f, 300f)), vx = 20f, vy = 0f)
        val advanced = SimulatorCore.advanceTarget(moving, 800f, 600f)
        assertTrue(advanced.vx < 0f) // reflected
    }

    @Test
    fun `target reflects off left wall`() {
        val moving = MovingTarget(SimTarget(1, Vec2(10f, 300f)), vx = -20f, vy = 0f)
        val advanced = SimulatorCore.advanceTarget(moving, 800f, 600f)
        assertTrue(advanced.vx > 0f)
    }

    @Test
    fun `target reflects off bottom wall`() {
        val moving = MovingTarget(SimTarget(1, Vec2(400f, 590f)), vx = 0f, vy = 20f)
        val advanced = SimulatorCore.advanceTarget(moving, 800f, 600f)
        assertTrue(advanced.vy < 0f)
    }

    @Test
    fun `target stays within arena bounds after advance`() {
        val moving = MovingTarget(SimTarget(1, Vec2(0f, 0f)), vx = -100f, vy = -100f)
        val advanced = SimulatorCore.advanceTarget(moving, 800f, 600f)
        assertTrue(advanced.target.center.x >= 0f)
        assertTrue(advanced.target.center.y >= 0f)
    }

    // -----------------------------------------------------------------------
    // makeTargets
    // -----------------------------------------------------------------------

    @Test
    fun `makeTargets returns requested count`() {
        val targets = SimulatorCore.makeTargets(5, 800f, 600f)
        assertEquals(5, targets.size)
    }

    @Test
    fun `makeTargets assigns sequential ids from startId`() {
        val targets = SimulatorCore.makeTargets(3, 800f, 600f, startId = 10)
        assertEquals(listOf(10, 11, 12), targets.map { it.target.id })
    }

    @Test
    fun `makeTargets positions within arena`() {
        val targets = SimulatorCore.makeTargets(10, 800f, 600f)
        targets.forEach { m ->
            assertTrue(m.target.center.x >= 0f && m.target.center.x <= 800f)
            assertTrue(m.target.center.y >= 0f && m.target.center.y <= 600f)
        }
    }

    // -----------------------------------------------------------------------
    // Session stats
    // -----------------------------------------------------------------------

    @Test
    fun `accuracy is 0 when no shots fired`() {
        assertEquals(0f, SessionStats().accuracy, 0.001f)
    }

    @Test
    fun `accuracy is 1 when all shots hit`() {
        val stats = SessionStats(totalShots = 10, headHits = 5, bodyHits = 5, misses = 0)
        assertEquals(1f, stats.accuracy, 0.001f)
    }

    @Test
    fun `head hit rate is correct`() {
        val stats = SessionStats(totalShots = 10, headHits = 3, bodyHits = 4, misses = 3)
        assertEquals(0.3f, stats.headHitRate, 0.001f)
    }

    // -----------------------------------------------------------------------
    // Aim assist
    // -----------------------------------------------------------------------

    @Test
    fun `aim assist snaps crosshair to head when inside snap radius`() {
        val target = SimTarget(1, Vec2(100f, 100f), bodyRadius = 30f, headRadius = 15f)
        val headCenter = target.headCenter // (100, 77.5)
        val crosshair  = Vec2(headCenter.x + 20f, headCenter.y) // within 40px snap
        val snapped    = SimulatorCore.applyAimAssist(crosshair, listOf(target), snapRadius = 40f)
        assertEquals(headCenter.x, snapped.x, 0.1f)
        assertEquals(headCenter.y, snapped.y, 0.1f)
    }

    @Test
    fun `aim assist leaves crosshair unchanged when outside snap radius`() {
        val target     = SimTarget(1, Vec2(100f, 100f))
        val crosshair  = Vec2(500f, 500f)
        val snapped    = SimulatorCore.applyAimAssist(crosshair, listOf(target), snapRadius = 40f)
        assertEquals(crosshair, snapped)
    }

    @Test
    fun `aim assist does nothing on empty target list`() {
        val crosshair = Vec2(200f, 200f)
        val snapped   = SimulatorCore.applyAimAssist(crosshair, emptyList())
        assertEquals(crosshair, snapped)
    }
}
