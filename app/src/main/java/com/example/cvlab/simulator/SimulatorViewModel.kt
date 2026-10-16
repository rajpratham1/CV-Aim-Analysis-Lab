package com.example.cvlab.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cvlab.geometry.Vec2
import com.example.cvlab.data.SessionRepository
import com.example.cvlab.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

data class SimulatorUiState(
    val targets: List<MovingTarget> = emptyList(),
    val crosshair: Vec2 = Vec2(180f, 180f),
    val stats: SessionStats = SessionStats(),
    val events: List<String> = listOf("Simulator ready — targets use synthetic coordinates."),
    val lastResult: String = "Move the crosshair and tap FIRE.",
    val settings: SimulatorSettings = SimulatorSettings(),
    val isPaused: Boolean = false,
    val arenaWidth: Float = 360f,
    val arenaHeight: Float = 360f,
    val nextTargetId: Int = 1,
    val frameIndex: Long = 0L,
    val showAimAssistLabel: Boolean = false,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class SimulatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SimulatorUiState())
    val uiState: StateFlow<SimulatorUiState> = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null

    init {
        // Collect global settings changes
        viewModelScope.launch {
            SettingsRepository.settings.collect { appSettings ->
                _uiState.update { current ->
                    val simSettings = current.settings.copy(
                        aimAssistEnabled = appSettings.aimAssistEnabled,
                        targetCount = appSettings.targetCount,
                        speedMultiplier = appSettings.speedMultiplier,
                        difficulty = appSettings.difficulty,
                    )
                    current.copy(settings = simSettings)
                }
                adjustTargetCount(appSettings.targetCount)
            }
        }
        spawnInitialTargets()
        startGameLoop()
    }

    // ------------------------------------------------------------------
    // Arena management
    // ------------------------------------------------------------------

    fun onArenaResized(width: Float, height: Float) {
        if (width < 1f || height < 1f) return
        _uiState.update { it.copy(arenaWidth = width, arenaHeight = height) }
        // Re-spawn if targets list is empty
        if (_uiState.value.targets.isEmpty()) spawnInitialTargets()
    }

    // ------------------------------------------------------------------
    // User actions
    // ------------------------------------------------------------------

    fun onCrosshairMoved(pos: Vec2) {
        val state = _uiState.value
        val effective = if (state.settings.aimAssistEnabled) {
            SimulatorCore.applyAimAssist(pos, state.targets.map { it.target })
        } else pos
        _uiState.update { it.copy(crosshair = effective) }
    }

    fun onFire() {
        val state = _uiState.value
        if (state.isPaused) return

        val crosshair = state.crosshair
        val (result, targetId) = SimulatorCore.classifyShot(crosshair, state.targets.map { it.target })

        val message = when (result) {
            ShotResult.HEAD_HIT  -> "HEAD HIT • target #$targetId"
            ShotResult.BODY_HIT  -> "BODY HIT • target #$targetId"
            ShotResult.MISS      -> "MISS • no hitbox at crosshair"
        }

        val updatedStats = state.stats.copy(
            totalShots = state.stats.totalShots + 1,
            headHits   = state.stats.headHits   + if (result == ShotResult.HEAD_HIT) 1 else 0,
            bodyHits   = state.stats.bodyHits   + if (result == ShotResult.BODY_HIT) 1 else 0,
            misses     = state.stats.misses     + if (result == ShotResult.MISS) 1 else 0,
        )

        SessionRepository.recordSimulatorStats(
            shots = updatedStats.totalShots,
            heads = updatedStats.headHits,
            bodies = updatedStats.bodyHits,
            misses = updatedStats.misses
        )

        _uiState.update {
            it.copy(
                stats      = updatedStats,
                lastResult = message,
                events     = (listOf(message) + it.events).take(20),
            )
        }
    }

    fun onPauseResume() {
        val paused = !_uiState.value.isPaused
        _uiState.update { it.copy(isPaused = paused) }
        if (!paused) startGameLoop()
    }

    fun onReset() {
        gameLoopJob?.cancel()
        val emptyStats = SessionStats()
        SessionRepository.recordSimulatorStats(0, 0, 0, 0)
        _uiState.update {
            it.copy(
                stats      = emptyStats,
                events     = listOf("Session reset."),
                lastResult = "Session reset — ready to fire.",
                isPaused   = false,
            )
        }
        spawnInitialTargets()
        startGameLoop()
    }

    fun onSettingsChanged(settings: SimulatorSettings) {
        _uiState.update { it.copy(settings = settings) }
        SettingsRepository.update {
            it.copy(
                aimAssistEnabled = settings.aimAssistEnabled,
                targetCount = settings.targetCount,
                speedMultiplier = settings.speedMultiplier,
                difficulty = settings.difficulty
            )
        }
        adjustTargetCount(settings.targetCount)
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private fun spawnInitialTargets() {
        val state = _uiState.value
        val count = state.settings.targetCount
        val targets = SimulatorCore.makeTargets(
            count       = count,
            arenaWidth  = state.arenaWidth,
            arenaHeight = state.arenaHeight,
            startId     = state.nextTargetId,
            speedMult   = state.settings.speedMultiplier,
            difficulty  = state.settings.difficulty,
        )
        _uiState.update {
            it.copy(
                targets      = targets,
                nextTargetId = it.nextTargetId + count,
            )
        }
    }

    private fun adjustTargetCount(desired: Int) {
        val state = _uiState.value
        val current = state.targets
        when {
            current.size > desired -> _uiState.update { it.copy(targets = current.take(desired)) }
            current.size < desired -> {
                val missing  = desired - current.size
                val newBatch = SimulatorCore.makeTargets(
                    count       = missing,
                    arenaWidth  = state.arenaWidth,
                    arenaHeight = state.arenaHeight,
                    startId     = state.nextTargetId,
                    speedMult   = state.settings.speedMultiplier,
                    difficulty  = state.settings.difficulty,
                )
                _uiState.update {
                    it.copy(
                        targets      = it.targets + newBatch,
                        nextTargetId = it.nextTargetId + missing,
                    )
                }
            }
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (true) {
                delay(16) // ~60 fps
                val state = _uiState.value
                if (state.isPaused) continue

                val advancedTargets = state.targets.map { moving ->
                    SimulatorCore.advanceTarget(
                        moving      = moving,
                        arenaWidth  = state.arenaWidth,
                        arenaHeight = state.arenaHeight,
                    )
                }
                _uiState.update { it.copy(targets = advancedTargets, frameIndex = it.frameIndex + 1) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}

// tiny private extension helpers — no external dependency
private fun Float.sign(): Float = if (this >= 0f) 1f else -1f
private fun Float.absNorm(): Float = kotlin.math.abs(this)
