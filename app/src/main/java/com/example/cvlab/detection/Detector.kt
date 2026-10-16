package com.example.cvlab.detection

import com.example.cvlab.geometry.BoundingBox
import com.example.cvlab.geometry.Vec2

// ---------------------------------------------------------------------------
// Detection result model
// ---------------------------------------------------------------------------

/**
 * A single object detection produced by any [Detector].
 *
 * Coordinates are in the image's pixel space unless [isNormalized] is true.
 */
data class DetectionResult(
    val box: BoundingBox,
    val label: String,
    val confidence: Float,
    val classId: Int = 0,
    val isNormalized: Boolean = false,
    val source: DetectionSource = DetectionSource.SYNTHETIC,
)

/** Describes where a detection came from. */
enum class DetectionSource {
    /** Real on-device model inference. */
    MODEL,
    /** Synthetic/demo data — clearly distinguished from real detections. */
    SYNTHETIC,
}

// ---------------------------------------------------------------------------
// Detector interface
// ---------------------------------------------------------------------------

/**
 * Common interface for all detectors (synthetic demo, TFLite model, etc.).
 *
 * Implementations must be thread-safe if called from a background dispatcher.
 */
interface Detector {
    val name: String
    val isReady: Boolean

    /**
     * Run detection on raw ARGB pixel data.
     *
     * @param argb          ARGB_8888 pixel array of length [width]*[height].
     * @param width         Image width in pixels.
     * @param height        Image height in pixels.
     * @param confidenceThreshold Minimum confidence to retain a result.
     * @return              List of detections, or empty when nothing is found.
     */
    suspend fun detect(
        argb: IntArray,
        width: Int,
        height: Int,
        confidenceThreshold: Float = 0.5f,
    ): List<DetectionResult>

    /** Release any native resources. No-op if not applicable. */
    fun release() {}
}

// ---------------------------------------------------------------------------
// Inference timing
// ---------------------------------------------------------------------------

data class InferenceStats(
    val lastLatencyMs: Long = 0L,
    val avgLatencyMs: Double = 0.0,
    val p50LatencyMs: Long = 0L,
    val p95LatencyMs: Long = 0L,
    val totalInferences: Int = 0,
)

class InferenceTimer {
    private val latencies = ArrayDeque<Long>(200)

    fun record(latencyMs: Long) {
        if (latencies.size >= 200) latencies.removeFirst()
        latencies.addLast(latencyMs)
    }

    fun stats(): InferenceStats {
        if (latencies.isEmpty()) return InferenceStats()
        val sorted = latencies.sorted()
        val n      = sorted.size
        return InferenceStats(
            lastLatencyMs   = latencies.last(),
            avgLatencyMs    = sorted.average(),
            p50LatencyMs    = sorted[n / 2],
            p95LatencyMs    = sorted[(n * 0.95).toInt().coerceIn(0, n - 1)],
            totalInferences = n,
        )
    }

    fun reset() = latencies.clear()
}

// ---------------------------------------------------------------------------
// Synthetic demonstration detector
// ---------------------------------------------------------------------------

/**
 * A demo detector that generates plausible bounding boxes from the
 * image dimensions without performing any real inference.
 *
 * All results carry [DetectionSource.SYNTHETIC] — they are NEVER reported as
 * real computer-vision detections.
 */
class SyntheticDemoDetector : Detector {
    override val name: String    = "Synthetic Demo (no real model)"
    override val isReady: Boolean = true

    override suspend fun detect(
        argb: IntArray,
        width: Int,
        height: Int,
        confidenceThreshold: Float,
    ): List<DetectionResult> {
        // Produce 2-4 plausible-looking boxes spread across the image.
        val rand  = java.util.Random(argb.size.toLong() + width + height)
        val count = 2 + rand.nextInt(3)
        return (0 until count).map { i ->
            val cx = (0.2f + rand.nextFloat() * 0.6f) * width
            val cy = (0.2f + rand.nextFloat() * 0.5f) * height
            val bw = (0.08f + rand.nextFloat() * 0.12f) * width
            val bh = bw * (1.4f + rand.nextFloat() * 0.6f)
            val conf = 0.55f + rand.nextFloat() * 0.40f
            DetectionResult(
                box        = BoundingBox.fromCenter(Vec2(cx, cy), bw, bh),
                label      = if (i == 0) "person" else "target",
                confidence = conf,
                classId    = i,
                source     = DetectionSource.SYNTHETIC,
            )
        }.filter { it.confidence >= confidenceThreshold }
    }
}

// ---------------------------------------------------------------------------
// TFLite model stub (model not bundled — documents expected format)
// ---------------------------------------------------------------------------

/**
 * Placeholder for a real TensorFlow Lite object-detection model.
 *
 * The application will not load any model automatically.  The user must
 * place a compatible `.tflite` file in the assets folder and configure the
 * model name in Settings.
 *
 * Expected model format:
 *   - Input tensor:  [1, inputH, inputW, 3] float32, normalised to [0,1]
 *   - Output tensors (SSD-style):
 *       [0]: boxes        [1, num_detections, 4] — [ymin, xmin, ymax, xmax] normalised
 *       [1]: classes      [1, num_detections]    — class index float32
 *       [2]: scores       [1, num_detections]    — confidence float32
 *       [3]: count        [1]                    — number of valid detections
 *
 * @param modelFileName  Asset filename, e.g. "detect.tflite".
 */
class TFLiteDetectorStub(private val modelFileName: String) : Detector {
    override val name: String     = "TFLite ($modelFileName) — NOT LOADED"
    override val isReady: Boolean = false

    override suspend fun detect(
        argb: IntArray,
        width: Int,
        height: Int,
        confidenceThreshold: Float,
    ): List<DetectionResult> {
        // Model is not loaded — return empty list instead of fabricating results.
        return emptyList()
    }
}
