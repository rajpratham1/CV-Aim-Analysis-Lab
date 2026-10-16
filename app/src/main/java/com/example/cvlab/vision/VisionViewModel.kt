package com.example.cvlab.vision

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cvlab.data.SessionRepository
import com.example.cvlab.data.SettingsRepository
import com.example.cvlab.detection.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Image processing pipeline steps
// ---------------------------------------------------------------------------

data class PipelineStep(
    val name: String,
    val description: String,
    val completed: Boolean = false,
    val durationMs: Long = 0L,
)

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

data class VisionUiState(
    val imageUri: Uri? = null,
    val originalBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
    val detections: List<DetectionResult> = emptyList(),
    val pipelineSteps: List<PipelineStep> = emptyList(),
    val inferenceStats: InferenceStats = InferenceStats(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val detectorName: String = "Synthetic Demo (no real model)",
    val confidenceThreshold: Float = 0.5f,
    val useSyntheticDetector: Boolean = true,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class VisionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VisionUiState())
    val uiState: StateFlow<VisionUiState> = _uiState.asStateFlow()

    private var detector: Detector = SyntheticDemoDetector()
    private val timer = InferenceTimer()

    init {
        // Collect model configuration and confidence threshold from settings
        viewModelScope.launch {
            SettingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(confidenceThreshold = settings.confidenceThreshold)
                }
                updateDetector(settings.modelName)
            }
        }
    }

    private fun updateDetector(modelName: String) {
        if (modelName.isBlank()) {
            detector.release()
            detector = SyntheticDemoDetector()
            _uiState.update {
                it.copy(
                    detectorName = detector.name,
                    useSyntheticDetector = true,
                )
            }
        } else {
            detector.release()
            val tflite = TFLiteDetector(
                modelFileName = modelName,
                assetManager = getApplication<Application>().assets
            )
            detector = tflite
            _uiState.update {
                it.copy(
                    detectorName = tflite.name,
                    useSyntheticDetector = !tflite.isReady,
                    errorMessage = if (!tflite.isReady) tflite.statusMessage else null
                )
            }
        }
    }

    fun onImageSelected(uri: Uri, bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                imageUri         = uri,
                originalBitmap   = bitmap,
                processedBitmap  = null,
                detections       = emptyList(),
                errorMessage     = null,
                pipelineSteps    = emptyList(),
            )
        }
    }

    fun onRunDetection() {
        val bitmap = _uiState.value.originalBitmap ?: return
        if (_uiState.value.isProcessing) return

        _uiState.update { it.copy(isProcessing = true, errorMessage = null, detections = emptyList()) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    runPipeline(bitmap)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isProcessing = false, errorMessage = "Pipeline error: ${e.message}")
                }
            }
        }
    }

    fun onThresholdChanged(value: Float) {
        _uiState.update { it.copy(confidenceThreshold = value) }
        SettingsRepository.setConfidenceThreshold(value)
    }

    fun onClearImage() {
        _uiState.update {
            it.copy(
                imageUri        = null,
                originalBitmap  = null,
                processedBitmap = null,
                detections      = emptyList(),
                pipelineSteps   = emptyList(),
                errorMessage    = null,
            )
        }
    }

    // ------------------------------------------------------------------
    // Pipeline
    // ------------------------------------------------------------------

    private suspend fun runPipeline(bitmap: Bitmap) {
        val steps = mutableListOf<PipelineStep>()

        fun updateSteps(list: List<PipelineStep>) = _uiState.update { it.copy(pipelineSteps = list.toList()) }

        // Step 1: validate image
        var t0 = System.currentTimeMillis()
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            _uiState.update { it.copy(isProcessing = false, errorMessage = "Invalid image dimensions.") }
            return
        }
        steps += PipelineStep("Validate", "Checked image size (${bitmap.width}×${bitmap.height})", true, System.currentTimeMillis() - t0)
        updateSteps(steps)

        // Step 2: verify detector availability
        if (!detector.isReady && detector !is SyntheticDemoDetector) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    errorMessage = "Detector not loaded: ${detector.name}. Check docs/MODEL_INTEGRATION.md for setup instructions, or clear model filename in Settings to use synthetic demo.",
                    pipelineSteps = steps + PipelineStep("Model Check", "Model is unavailable", false)
                )
            }
            return
        }

        // Step 3: resize to input tensor size (300×300 for standard SSD)
        t0 = System.currentTimeMillis()
        val targetSize = 300
        val resized = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        steps += PipelineStep("Resize", "Resized to ${targetSize}×${targetSize} (model input)", true, System.currentTimeMillis() - t0)
        updateSteps(steps)

        // Step 4: extract pixel array
        t0 = System.currentTimeMillis()
        val pixels = IntArray(resized.width * resized.height)
        resized.getPixels(pixels, 0, resized.width, 0, 0, resized.width, resized.height)
        steps += PipelineStep("Extract Pixels", "Extracted ARGB pixel array (${pixels.size} pixels)", true, System.currentTimeMillis() - t0)
        updateSteps(steps)

        // Step 5: detect
        t0 = System.currentTimeMillis()
        val rawDetections = detector.detect(
            pixels, resized.width, resized.height,
            _uiState.value.confidenceThreshold,
        )
        val latency = System.currentTimeMillis() - t0
        timer.record(latency)

        // Scale detections back to original image coordinates
        val scaleX = bitmap.width.toFloat() / resized.width
        val scaleY = bitmap.height.toFloat() / resized.height
        val scaledDetections = rawDetections.map { d ->
            d.copy(box = d.box.scale(scaleX, scaleY))
        }

        steps += PipelineStep(
            "Inference",
            "${scaledDetections.size} detections in ${latency}ms (${detector.name})",
            true, latency,
        )
        updateSteps(steps)

        // Report to global session analytics
        val isSynthetic = detector is SyntheticDemoDetector
        val currentStats = timer.stats()
        SessionRepository.recordVisionStats(
            framesProcessed = currentStats.totalInferences.toLong(),
            avgLatencyMs    = currentStats.avgLatencyMs,
            p50Ms           = currentStats.p50LatencyMs,
            p95Ms           = currentStats.p95LatencyMs,
            detectorReady   = detector.isReady,
            isSynthetic     = isSynthetic
        )

        // Step 6: done
        _uiState.update {
            it.copy(
                isProcessing   = false,
                detections     = scaledDetections,
                inferenceStats = currentStats,
                pipelineSteps  = steps,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector.release()
    }
}
