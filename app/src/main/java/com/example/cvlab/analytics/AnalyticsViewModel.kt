package com.example.cvlab.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cvlab.data.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyticsState(
    // Simulator stats (injected from SimulatorViewModel externally)
    val totalShots:   Int   = 0,
    val headHits:     Int   = 0,
    val bodyHits:     Int   = 0,
    val misses:       Int   = 0,

    // Vision / inference metrics
    val framesProcessed: Long    = 0L,
    val avgLatencyMs:    Double  = 0.0,
    val p50LatencyMs:    Long    = 0L,
    val p95LatencyMs:    Long    = 0L,
    val detectorReady:   Boolean = false,

    // Tracking metrics
    val trackCount:     Int   = 0,
    val meanTrackError: Float = Float.NaN,
    val trackContinuity: Float = 0f,

    // System
    val cpuUsagePct:    Float = Float.NaN,  // placeholder — not measurable without PACKAGE_USAGE_STATS
    val memoryUsageMb:  Long  = 0L,

    // Meta
    val isSyntheticData: Boolean = true,
)

class AnalyticsViewModel : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    init {
        // Collect shared session analytics from SessionRepository
        viewModelScope.launch {
            SessionRepository.analyticsState.collect { sharedState ->
                _state.update { current ->
                    sharedState.copy(memoryUsageMb = current.memoryUsageMb)
                }
            }
        }

        // Poll memory usage every 5 seconds
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                val rt = Runtime.getRuntime()
                val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                _state.update { it.copy(memoryUsageMb = usedMb) }
            }
        }
    }

    /** Called by other ViewModels (or NavHost) to push fresh simulator stats. */
    fun updateSimulatorStats(shots: Int, heads: Int, bodies: Int, misses: Int) {
        SessionRepository.recordSimulatorStats(shots, heads, bodies, misses)
    }

    fun updateVisionStats(framesProcessed: Long, avgMs: Double, p50: Long, p95: Long, detectorReady: Boolean = false, isSynthetic: Boolean = true) {
        SessionRepository.recordVisionStats(framesProcessed, avgMs, p50, p95, detectorReady, isSynthetic)
    }

    fun updateTrackingStats(count: Int, meanError: Float, continuity: Float = 1.0f) {
        SessionRepository.recordTrackingStats(count, meanError, continuity)
    }
}
