package com.example.cvlab.detection

import android.content.res.AssetManager
import com.example.cvlab.geometry.BoundingBox
import com.example.cvlab.geometry.Vec2
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Real TensorFlow Lite Object Detector implementation.
 *
 * Supports standard SSD MobileNet / EfficientDet architectures.
 * Safely handles missing models, corrupted buffers, and mismatched tensor dimensions.
 */
class TFLiteDetector(
    val modelFileName: String = "",
    modelBuffer: ByteBuffer? = null,
    assetManager: AssetManager? = null,
    numThreads: Int = 2,
) : Detector {

    override val name: String
        get() = if (isReady) "TFLite SSD ($modelFileName)" else "TFLite ($modelFileName) — UNAVAILABLE"

    private var interpreter: Interpreter? = null
    private var _isReady: Boolean = false
    override val isReady: Boolean get() = _isReady

    var statusMessage: String = ""
        private set

    private var inputWidth: Int = 300
    private var inputHeight: Int = 300
    private var isFloatModel: Boolean = true
    private var numDetections: Int = 10

    init {
        loadModel(modelFileName, modelBuffer, assetManager, numThreads)
    }

    private fun loadModel(
        fileName: String,
        providedBuffer: ByteBuffer?,
        assets: AssetManager?,
        numThreads: Int,
    ) {
        if (fileName.isEmpty() && providedBuffer == null) {
            _isReady = false
            statusMessage = "No TFLite model specified. Place a .tflite file in assets and configure in Settings."
            return
        }

        try {
            val buffer = providedBuffer ?: run {
                requireNotNull(assets) { "AssetManager must be provided when loading from asset filename" }
                loadAssetBuffer(assets, fileName)
            }

            val options = Interpreter.Options().apply {
                setNumThreads(numThreads)
            }

            val interp = Interpreter(buffer, options)

            // Inspect input tensor (tensor 0)
            val inTensor = interp.getInputTensor(0)
            val inShape = inTensor.shape() // [1, H, W, 3]
            if (inShape.size >= 4) {
                inputHeight = inShape[1]
                inputWidth = inShape[2]
            }
            isFloatModel = inTensor.dataType() == DataType.FLOAT32

            // Inspect output boxes tensor (tensor 0)
            val outTensor = interp.getOutputTensor(0)
            val outShape = outTensor.shape() // [1, num_detections, 4]
            if (outShape.size >= 2) {
                numDetections = outShape[1]
            }

            interpreter = interp
            _isReady = true
            statusMessage = "Model loaded successfully ($inputWidth×$inputHeight, max $numDetections detections)."
        } catch (e: Throwable) {
            _isReady = false
            interpreter = null
            statusMessage = "Failed to load model '$fileName': ${e.message}"
        }
    }

    private fun loadAssetBuffer(assets: AssetManager, fileName: String): ByteBuffer {
        return try {
            val fileDescriptor = assets.openFd(fileName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            // Fallback for compressed assets
            assets.open(fileName).use { stream ->
                val bytes = stream.readBytes()
                val directBuffer = ByteBuffer.allocateDirect(bytes.size)
                directBuffer.order(ByteOrder.nativeOrder())
                directBuffer.put(bytes)
                directBuffer.rewind()
                directBuffer
            }
        }
    }

    override suspend fun detect(
        argb: IntArray,
        width: Int,
        height: Int,
        confidenceThreshold: Float,
    ): List<DetectionResult> {
        val interp = interpreter
        if (!_isReady || interp == null || width <= 0 || height <= 0 || argb.size < width * height) {
            return emptyList()
        }

        return try {
            // 1. Prepare preprocessed input buffer
            val bytesPerChannel = if (isFloatModel) 4 else 1
            val inputBuffer = ByteBuffer.allocateDirect(1 * inputHeight * inputWidth * 3 * bytesPerChannel)
            inputBuffer.order(ByteOrder.nativeOrder())

            // Resize / sample from ARGB image to input tensor dimensions
            for (y in 0 until inputHeight) {
                val srcY = (y * height) / inputHeight
                val rowOffset = srcY * width
                for (x in 0 until inputWidth) {
                    val srcX = (x * width) / inputWidth
                    val pixel = argb[rowOffset + srcX]
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    if (isFloatModel) {
                        inputBuffer.putFloat(r / 255.0f)
                        inputBuffer.putFloat(g / 255.0f)
                        inputBuffer.putFloat(b / 255.0f)
                    } else {
                        inputBuffer.put(r.toByte())
                        inputBuffer.put(g.toByte())
                        inputBuffer.put(b.toByte())
                    }
                }
            }
            inputBuffer.rewind()

            // 2. Prepare output tensor maps (SSD Detection format)
            val outputMap = HashMap<Int, Any>()
            val outBoxes = Array(1) { Array(numDetections) { FloatArray(4) } }
            val outClasses = Array(1) { FloatArray(numDetections) }
            val outScores = Array(1) { FloatArray(numDetections) }
            val outCount = FloatArray(1)

            outputMap[0] = outBoxes
            outputMap[1] = outClasses
            outputMap[2] = outScores
            outputMap[3] = outCount

            // 3. Run inference
            interp.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)

            // 4. Decode results
            val count = outCount[0].toInt().coerceIn(0, numDetections)
            val results = mutableListOf<DetectionResult>()

            for (i in 0 until count) {
                val score = outScores[0][i]
                if (score >= confidenceThreshold) {
                    val rawBox = outBoxes[0][i]
                    val ymin = rawBox[0].coerceIn(0f, 1f)
                    val xmin = rawBox[1].coerceIn(0f, 1f)
                    val ymax = rawBox[2].coerceIn(0f, 1f)
                    val xmax = rawBox[3].coerceIn(0f, 1f)

                    val boxW = ((xmax - xmin) * width).coerceAtLeast(1f)
                    val boxH = ((ymax - ymin) * height).coerceAtLeast(1f)
                    val boxLeft = xmin * width
                    val boxTop = ymin * height

                    val classId = outClasses[0][i].toInt()
                    val label = COCO_CLASSES.getOrElse(classId) { "class_$classId" }

                    results += DetectionResult(
                        box = BoundingBox(Vec2(boxLeft, boxTop), boxW, boxH),
                        label = label,
                        confidence = score,
                        classId = classId,
                        isNormalized = false,
                        source = DetectionSource.MODEL,
                    )
                }
            }
            results
        } catch (e: Throwable) {
            statusMessage = "Inference error: ${e.message}"
            emptyList()
        }
    }

    override fun release() {
        try {
            interpreter?.close()
        } catch (_: Throwable) {}
        interpreter = null
        _isReady = false
    }

    companion object {
        val COCO_CLASSES = listOf(
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
            "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
            "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
            "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
            "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
            "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse",
            "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator",
            "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush", "target"
        )
    }
}
