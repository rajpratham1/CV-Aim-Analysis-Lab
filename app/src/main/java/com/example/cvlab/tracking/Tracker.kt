package com.example.cvlab.tracking

import com.example.cvlab.detection.DetectionResult
import com.example.cvlab.geometry.Vec2
import kotlin.math.abs

// ---------------------------------------------------------------------------
// Domain types
// ---------------------------------------------------------------------------

/** A single detection used as input to the tracker. */
data class Detection(
    val boundingBoxCenter: Vec2,
    val width: Float,
    val height: Float,
    val confidence: Float,
    val frameTimestampMs: Long,
    val classLabel: String = "target",
)

/** Convert a vision Detector result into tracker Detection. */
fun DetectionResult.toTrackingDetection(frameTimestampMs: Long): Detection = Detection(
    boundingBoxCenter = box.center,
    width             = box.width,
    height            = box.height,
    confidence        = confidence,
    frameTimestampMs  = frameTimestampMs,
    classLabel        = label,
)

/** A managed track that persists across frames. */
data class Track(
    val id: Int,
    val position: Vec2,
    val velocity: Vec2,                 // pixels/frame estimate
    val history: List<Vec2>,            // last N positions
    val confidence: Float,
    val lastSeenFrameMs: Long,
    val age: Int,                       // frames since creation
    val missingFrames: Int,             // consecutive frames without a match
    val isConfirmed: Boolean,           // true after min-hit threshold
)

// ---------------------------------------------------------------------------
// Kalman-filter state (1-D, applied independently to x and y)
// ---------------------------------------------------------------------------

/**
 * Minimal 1-D constant-velocity Kalman filter.
 *
 * State vector: [position, velocity].
 * Measurement:  [position].
 *
 * @param processNoise   Q — how much the velocity can change per frame.
 * @param measurementNoise R — sensor noise standard deviation.
 */
class KalmanFilter1D(
    initialPosition: Float,
    initialVelocity: Float = 0f,
    private val processNoise: Float = 1f,
    private val measurementNoise: Float = 4f,
) {
    private var pos = initialPosition
    private var vel = initialVelocity

    // Covariance (simplified — diagonal only)
    private var pPos = 100f
    private var pVel = 100f

    /** Predict next state (no measurement). Returns predicted position. */
    fun predict(): Float {
        pos += vel
        pPos += pVel + processNoise
        pVel += processNoise
        return pos
    }

    /** Update with a new measurement. Returns corrected position. */
    fun update(measurement: Float): Float {
        val kPos = pPos / (pPos + measurementNoise)
        val kVel = pVel / (pPos + measurementNoise)

        val innovation = measurement - pos
        pos += kPos * innovation
        vel += kVel * innovation

        pPos = (1f - kPos) * pPos
        pVel = (1f - kVel) * pVel

        return pos
    }

    fun position(): Float = pos
    fun velocity(): Float = vel
}

// ---------------------------------------------------------------------------
// Multi-object tracker
// ---------------------------------------------------------------------------

/**
 * Simple IoU-distance multi-object tracker with optional Kalman prediction.
 *
 * Algorithm:
 * 1. Predict each existing track's next position.
 * 2. Compute cost matrix (Euclidean distance) between predictions and detections.
 * 3. Greedy nearest-neighbour assignment (or Hungarian in a future upgrade).
 * 4. Create new tracks for unmatched detections.
 * 5. Increment [Track.missingFrames] for unmatched tracks.
 * 6. Expire tracks that exceed [maxMissingFrames].
 */
class MultiObjectTracker(
    private val maxMissingFrames: Int = 5,
    private val minHitsToConfirm: Int = 3,
    private val maxAssociationDistance: Float = 80f,
    private val useKalman: Boolean = true,
    private val historySize: Int = 30,
) {
    private var nextId = 1

    /** Live tracks — not thread-safe; call from a single coroutine. */
    private val tracks = mutableListOf<Track>()

    /** Per-track Kalman filters (x and y independently). */
    private val kalmanX = mutableMapOf<Int, KalmanFilter1D>()
    private val kalmanY = mutableMapOf<Int, KalmanFilter1D>()

    // ------------------------------------------------------------------

    /**
     * Process one frame's [detections] and return the updated track list.
     *
     * @param frameMs Wall-clock timestamp of this frame in milliseconds.
     */
    fun update(detections: List<Detection>, frameMs: Long): List<Track> {
        // 1. Predict
        val predicted = mutableListOf<Pair<Int, Vec2>>() // (trackId, predictedPos)
        for (t in tracks) {
            val px = if (useKalman) kalmanX[t.id]?.predict() ?: (t.position.x + t.velocity.x) else t.position.x + t.velocity.x
            val py = if (useKalman) kalmanY[t.id]?.predict() ?: (t.position.y + t.velocity.y) else t.position.y + t.velocity.y
            predicted.add(t.id to Vec2(px, py))
        }

        // 2. Greedy assignment
        val matchedDetections = mutableSetOf<Int>()
        val matchedTracks     = mutableSetOf<Int>()
        val assignments       = mutableMapOf<Int, Int>() // trackId → detectionIndex

        for ((trackId, predPos) in predicted) {
            var bestDist = maxAssociationDistance
            var bestDet  = -1
            detections.forEachIndexed { di, det ->
                if (di in matchedDetections) return@forEachIndexed
                val dist = predPos.distanceTo(det.boundingBoxCenter)
                if (dist < bestDist) { bestDist = dist; bestDet = di }
            }
            if (bestDet >= 0) {
                assignments[trackId] = bestDet
                matchedDetections += bestDet
                matchedTracks     += trackId
            }
        }

        // 3. Update matched tracks
        val updatedTracks = tracks.map { t ->
            val di = assignments[t.id]
            if (di != null) {
                val det = detections[di]
                val mx  = if (useKalman) kalmanX[t.id]!!.update(det.boundingBoxCenter.x) else det.boundingBoxCenter.x
                val my  = if (useKalman) kalmanY[t.id]!!.update(det.boundingBoxCenter.y) else det.boundingBoxCenter.y
                val newPos = Vec2(mx, my)
                val vel    = newPos - t.position
                val newHistory = (t.history + newPos).takeLast(historySize)
                t.copy(
                    position       = newPos,
                    velocity       = vel,
                    history        = newHistory,
                    confidence     = det.confidence,
                    lastSeenFrameMs = frameMs,
                    age            = t.age + 1,
                    missingFrames  = 0,
                    isConfirmed    = t.age + 1 >= minHitsToConfirm,
                )
            } else {
                // Unmatched — advance missing counter
                t.copy(missingFrames = t.missingFrames + 1, age = t.age + 1)
            }
        }.filter { it.missingFrames <= maxMissingFrames }

        // 4. Create new tracks for unmatched detections
        val newTracks = mutableListOf<Track>()
        detections.forEachIndexed { di, det ->
            if (di !in matchedDetections) {
                val id = nextId++
                kalmanX[id] = KalmanFilter1D(det.boundingBoxCenter.x)
                kalmanY[id] = KalmanFilter1D(det.boundingBoxCenter.y)
                newTracks += Track(
                    id              = id,
                    position        = det.boundingBoxCenter,
                    velocity        = Vec2.Zero,
                    history         = listOf(det.boundingBoxCenter),
                    confidence      = det.confidence,
                    lastSeenFrameMs = frameMs,
                    age             = 1,
                    missingFrames   = 0,
                    isConfirmed     = minHitsToConfirm <= 1,
                )
            }
        }

        // 5. Prune Kalman state for removed tracks
        val survivingIds = (updatedTracks + newTracks).map { it.id }.toSet()
        kalmanX.keys.retainAll(survivingIds)
        kalmanY.keys.retainAll(survivingIds)

        tracks.clear()
        tracks.addAll(updatedTracks + newTracks)
        return tracks.toList()
    }

    /** Returns a copy of current tracks (snapshot). */
    fun currentTracks(): List<Track> = tracks.toList()

    /** Resets all state. */
    fun reset() {
        tracks.clear()
        kalmanX.clear()
        kalmanY.clear()
        nextId = 1
    }
}

// ---------------------------------------------------------------------------
// Tracking error metrics
// ---------------------------------------------------------------------------

data class TrackingMetrics(
    val meanPositionError: Float,    // average Euclidean distance, pixels
    val trackContinuity: Float,      // fraction of frames where ID was maintained
    val identitySwitches: Int,
    val totalFrames: Int,
    val lostTracks: Int,
)

/**
 * Computes mean position error between predicted positions and ground-truth
 * positions over a sequence of frame observations.
 *
 * For multi-target frames, matches each predicted track position to the closest
 * ground-truth target position in that frame to ensure valid metrics.
 *
 * @param predicted   List of (frameIndex → position) pairs from the tracker.
 * @param groundTruth List of (frameIndex → position) pairs from the simulator.
 */
fun computePositionError(
    predicted: List<Pair<Int, Vec2>>,
    groundTruth: List<Pair<Int, Vec2>>,
): Float {
    if (predicted.isEmpty() || groundTruth.isEmpty()) return Float.NaN
    val gtByFrame = groundTruth.groupBy({ it.first }, { it.second })
    var sum = 0f
    var count = 0
    for ((frame, pos) in predicted) {
        val frameGts = gtByFrame[frame] ?: continue
        if (frameGts.isEmpty()) continue
        val minDist = frameGts.minOf { gtPos -> pos.distanceTo(gtPos) }
        sum += minDist
        count++
    }
    return if (count == 0) Float.NaN else sum / count
}
