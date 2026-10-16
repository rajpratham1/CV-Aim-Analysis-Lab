package com.example.cvlab.simulator

import com.example.cvlab.geometry.HitZone
import com.example.cvlab.geometry.Vec2
import com.example.cvlab.geometry.classifyCircularHit
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Domain model
// ---------------------------------------------------------------------------

/**
 * A single simulated target with circular body and head hitboxes.
 *
 * The head circle is centred 75 % of [bodyRadius] above [center].
 */
data class SimTarget(
    val id: Int,
    val center: Vec2,
    val bodyRadius: Float = 28f,
    val headRadius: Float = 14f,
    val isAlive: Boolean = true,
) {
    /** Computed head-circle centre. */
    val headCenter: Vec2 get() = Vec2(center.x, center.y - bodyRadius * 0.75f)
}

/** Velocity attached to a [SimTarget] for the game loop. */
data class MovingTarget(
    val target: SimTarget,
    val vx: Float,
    val vy: Float,
)

enum class ShotResult { HEAD_HIT, BODY_HIT, MISS }

data class ShotEvent(
    val frameIndex: Long,
    val crosshairPos: Vec2,
    val result: ShotResult,
    val targetId: Int?,
    val message: String,
)

data class SessionStats(
    val totalShots: Int = 0,
    val headHits: Int = 0,
    val bodyHits: Int = 0,
    val misses: Int = 0,
) {
    val accuracy: Float
        get() = if (totalShots == 0) 0f else (headHits + bodyHits).toFloat() / totalShots

    val headHitRate: Float
        get() = if (totalShots == 0) 0f else headHits.toFloat() / totalShots
}

data class SimulatorSettings(
    val targetCount: Int = 4,
    val speedMultiplier: Float = 1.5f,
    val aimAssistEnabled: Boolean = false,
    val difficulty: Difficulty = Difficulty.NORMAL,
)

enum class Difficulty(val speedScale: Float, val targetSizeScale: Float) {
    EASY(0.7f, 1.3f),
    NORMAL(1.0f, 1.0f),
    HARD(1.6f, 0.8f),
    EXPERT(2.4f, 0.65f),
}

// ---------------------------------------------------------------------------
// Core simulation logic  (no Android / Compose dependencies)
// ---------------------------------------------------------------------------

object SimulatorCore {

    /**
     * Classifies a shot fired at [crosshair] against [targets].
     *
     * Checks head hitboxes first (higher priority), then body hitboxes.
     * Returns the best [ShotResult] and the matching target id (null on miss).
     */
    fun classifyShot(crosshair: Vec2, targets: List<SimTarget>): Pair<ShotResult, Int?> {
        // Priority 1: head hits
        for (t in targets) {
            if (!t.isAlive) continue
            if (classifyCircularHit(crosshair, t.center, t.bodyRadius, t.headRadius) == HitZone.HEAD) {
                return ShotResult.HEAD_HIT to t.id
            }
        }
        // Priority 2: body hits
        for (t in targets) {
            if (!t.isAlive) continue
            if (classifyCircularHit(crosshair, t.center, t.bodyRadius, t.headRadius) == HitZone.BODY) {
                return ShotResult.BODY_HIT to t.id
            }
        }
        return ShotResult.MISS to null
    }

    /**
     * Advances a [MovingTarget] one physics tick.
     *
     * Reflects velocity when the target would leave the arena and clamps
     * the position to stay within bounds.
     *
     * @param margin   Minimum distance from arena edges.
     * @param topMargin Extra top margin (e.g. for UI header space).
     */
    fun advanceTarget(
        moving: MovingTarget,
        arenaWidth: Float,
        arenaHeight: Float,
        margin: Float = 40f,
        topMargin: Float = 60f,
    ): MovingTarget {
        var vx = moving.vx
        var vy = moving.vy
        var x  = moving.target.center.x + vx
        var y  = moving.target.center.y + vy

        val minX = margin
        val maxX = (arenaWidth - margin).coerceAtLeast(minX)
        val minY = topMargin + margin
        val maxY = (arenaHeight - margin).coerceAtLeast(minY)

        if (x < minX || x > maxX) { vx = -vx; x = x.coerceIn(minX, maxX) }
        if (y < minY || y > maxY) { vy = -vy; y = y.coerceIn(minY, maxY) }

        return moving.copy(target = moving.target.copy(center = Vec2(x, y)), vx = vx, vy = vy)
    }

    /**
     * Creates [count] randomly placed [MovingTarget]s within the arena.
     *
     * @param startId       First target ID (increments for each new target).
     * @param speedMult     Overall speed multiplier from settings.
     * @param difficulty    Adjusts target sizes and speed.
     */
    fun makeTargets(
        count: Int,
        arenaWidth: Float,
        arenaHeight: Float,
        startId: Int = 1,
        speedMult: Float = 1.5f,
        difficulty: Difficulty = Difficulty.NORMAL,
        rand: Random = Random.Default,
    ): List<MovingTarget> {
        val safeW = arenaWidth.coerceAtLeast(320f)
        val safeH = arenaHeight.coerceAtLeast(320f)
        val baseBodyR = 28f * difficulty.targetSizeScale
        val baseHeadR = 14f * difficulty.targetSizeScale

        return (0 until count).map { i ->
            val x  = rand.nextFloat() * (safeW * 0.65f) + safeW * 0.17f
            val y  = rand.nextFloat() * (safeH * 0.45f) + safeH * 0.28f
            val sp = speedMult * difficulty.speedScale
            val vx = (rand.nextFloat() * 2f + 0.7f) * sp * if (rand.nextBoolean()) 1 else -1
            val vy = (rand.nextFloat() * 1.5f + 0.4f) * sp * if (rand.nextBoolean()) 1 else -1
            MovingTarget(
                SimTarget(startId + i, Vec2(x, y), baseBodyR, baseHeadR),
                vx, vy
            )
        }
    }

    /**
     * Optional aim-assist: returns the closest target centre (head or body)
     * within [snapRadius] of [crosshair].  Returns [crosshair] unchanged when
     * no target is close enough.
     *
     * This function operates only inside the simulator and is clearly labelled
     * as synthetic assistance — not a game-overlay or input-injection mechanism.
     */
    fun applyAimAssist(
        crosshair: Vec2,
        targets: List<SimTarget>,
        snapRadius: Float = 40f,
    ): Vec2 {
        var bestDist = Float.MAX_VALUE
        var bestPos  = crosshair
        for (t in targets) {
            if (!t.isAlive) continue
            // Try snapping to head centre first, then body centre
            for (candidate in listOf(t.headCenter, t.center)) {
                val d = crosshair.distanceTo(candidate)
                if (d < snapRadius && d < bestDist) {
                    bestDist = d
                    bestPos  = candidate
                }
            }
        }
        return bestPos
    }
}
