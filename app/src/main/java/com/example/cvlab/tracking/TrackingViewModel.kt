package com.example.cvlab.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cvlab.geometry.Vec2
import com.example.cvlab.simulator.*
import com.example.cvlab.data.SessionRepository
import com.example.cvlab.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrackingUiState(
    val simulatorTargets: List<MovingTarget> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val frameCount: Long = 0L,
    val arenaWidth: Float = 360f,
    val arenaHeight: Float = 360f,
    val useKalman: Boolean = true,
    val isRunning: Boolean = false,
    val meanError: Float = Float.NaN,
    val totalTracks: Int = 0,
    val lostTracks: Int = 0,
    val settings: SimulatorSettings = SimulatorSettings(targetCount = 3),
    val isSyntheticData: Boolean = true,
)

class TrackingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private var tracker = MultiObjectTracker(useKalman = true)
    private var loopJob: Job? = null

    // Ground-truth and prediction history for error computation
    private val gtHistory  = mutableListOf<Pair<Int, Vec2>>()   // (frame, pos)
    private val predHistory = mutableListOf<Pair<Int, Vec2>>()

    init {
        viewModelScope.launch {
            SettingsRepository.settings.collect { appSettings ->
                if (_uiState.value.useKalman != appSettings.kalmanEnabled) {
                    tracker = MultiObjectTracker(useKalman = appSettings.kalmanEnabled)
                    _uiState.update { it.copy(useKalman = appSettings.kalmanEnabled, tracks = emptyList()) }
                }
            }
        }
        startSimulation()
    }

    fun onArenaResized(w: Float, h: Float) {
        if (w < 1f || h < 1f) return
        _uiState.update { it.copy(arenaWidth = w, arenaHeight = h) }
    }

    fun onToggleKalman() {
        val k = !_uiState.value.useKalman
        tracker = MultiObjectTracker(useKalman = k)
        SettingsRepository.setKalman(k)
        _uiState.update { it.copy(useKalman = k, tracks = emptyList()) }
    }

    fun onReset() {
        loopJob?.cancel()
        tracker.reset()
        gtHistory.clear()
        predHistory.clear()
        _uiState.update {
            it.copy(
                simulatorTargets = emptyList(),
                tracks           = emptyList(),
                frameCount       = 0L,
                meanError        = Float.NaN,
                totalTracks      = 0,
                lostTracks       = 0,
                isRunning        = false,
            )
        }
        startSimulation()
    }

    fun onPauseResume() {
        val running = !_uiState.value.isRunning
        _uiState.update { it.copy(isRunning = running) }
        if (running) startSimulation()
    }

    private fun startSimulation() {
        loopJob?.cancel()
        _uiState.update { it.copy(isRunning = true) }

        loopJob = viewModelScope.launch {
            var targets = SimulatorCore.makeTargets(
                count      = _uiState.value.settings.targetCount,
                arenaWidth = _uiState.value.arenaWidth,
                arenaHeight = _uiState.value.arenaHeight,
                speedMult  = _uiState.value.settings.speedMultiplier,
            )

            while (true) {
                delay(33) // ~30 fps for tracking lab
                val state = _uiState.value
                if (!state.isRunning) continue

                val frameMs = System.currentTimeMillis()
                val frame   = state.frameCount.toInt()

                // Advance simulator targets (ground truth)
                targets = targets.map { m ->
                    SimulatorCore.advanceTarget(m, state.arenaWidth, state.arenaHeight)
                }

                // Record ground truth positions
                targets.forEach { m -> gtHistory += frame to m.target.center }

                // Build synthetic detections with slight noise (simulates detector jitter)
                val detections = targets.map { m ->
                    val noise = Vec2(
                        (Math.random() * 6 - 3).toFloat(),
                        (Math.random() * 6 - 3).toFloat(),
                    )
                    Detection(
                        boundingBoxCenter = m.target.center + noise,
                        width             = m.target.bodyRadius * 2,
                        height            = m.target.bodyRadius * 2.8f,
                        confidence        = 0.85f + (Math.random() * 0.14).toFloat(),
                        frameTimestampMs  = frameMs,
                    )
                }

                // Update tracker
                val updatedTracks = tracker.update(detections, frameMs)

                // Record predicted positions
                updatedTracks.forEach { t -> predHistory += frame to t.position }

                // Compute rolling error every 30 frames
                val err = if (predHistory.size >= 10) {
                    val recent = predHistory.takeLast(30)
                    computePositionError(recent, gtHistory.takeLast(30 * targets.size))
                } else Float.NaN

                SessionRepository.recordTrackingStats(updatedTracks.size, err, 1.0f)

                _uiState.update {
                    it.copy(
                        simulatorTargets = targets,
                        tracks           = updatedTracks,
                        frameCount       = it.frameCount + 1,
                        meanError        = err,
                        totalTracks      = tracker.currentTracks().size,
                    )
                }
            }
        }
    }

    /**
     * Feed real detections from an external detector into the tracker.
     * Pauses synthetic target simulation to focus on real observations.
     */
    fun trackRealDetections(detections: List<Detection>, frameMs: Long): List<Track> {
        loopJob?.cancel()
        val updatedTracks = tracker.update(detections, frameMs)
        _uiState.update {
            it.copy(
                tracks = updatedTracks,
                totalTracks = tracker.currentTracks().size,
                isSyntheticData = false,
                isRunning = false
            )
        }
        SessionRepository.recordTrackingStats(updatedTracks.size, Float.NaN, 1.0f)
        return updatedTracks
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
    }
}
