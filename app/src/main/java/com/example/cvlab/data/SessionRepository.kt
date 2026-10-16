package com.example.cvlab.data

import com.example.cvlab.analytics.AnalyticsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object SessionRepository {
    private val _analyticsState = MutableStateFlow(AnalyticsState())
    val analyticsState: StateFlow<AnalyticsState> = _analyticsState.asStateFlow()

    fun recordSimulatorStats(shots: Int, heads: Int, bodies: Int, misses: Int) {
        _analyticsState.update {
            it.copy(
                totalShots = shots,
                headHits = heads,
                bodyHits = bodies,
                misses = misses
            )
        }
    }

    fun recordVisionStats(
        framesProcessed: Long,
        avgLatencyMs: Double,
        p50Ms: Long,
        p95Ms: Long,
        detectorReady: Boolean,
        isSynthetic: Boolean
    ) {
        _analyticsState.update {
            it.copy(
                framesProcessed = framesProcessed,
                avgLatencyMs = avgLatencyMs,
                p50LatencyMs = p50Ms,
                p95LatencyMs = p95Ms,
                detectorReady = detectorReady,
                isSyntheticData = isSynthetic
            )
        }
    }

    fun recordTrackingStats(trackCount: Int, meanError: Float, trackContinuity: Float) {
        _analyticsState.update {
            it.copy(
                trackCount = trackCount,
                meanTrackError = meanError,
                trackContinuity = trackContinuity
            )
        }
    }
}
