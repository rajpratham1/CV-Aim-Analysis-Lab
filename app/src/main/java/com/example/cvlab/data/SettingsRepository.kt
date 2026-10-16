package com.example.cvlab.data

import com.example.cvlab.simulator.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppSettings(
    val darkTheme: Boolean = true,
    val aimAssistEnabled: Boolean = false,
    val kalmanEnabled: Boolean = true,
    val modelName: String = "",
    val confidenceThreshold: Float = 0.5f,
    val targetCount: Int = 4,
    val speedMultiplier: Float = 1.5f,
    val difficulty: Difficulty = Difficulty.NORMAL,
)

object SettingsRepository {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        _settings.update(transform)
    }

    fun setAimAssist(enabled: Boolean) {
        _settings.update { it.copy(aimAssistEnabled = enabled) }
    }

    fun setKalman(enabled: Boolean) {
        _settings.update { it.copy(kalmanEnabled = enabled) }
    }

    fun setModelName(name: String) {
        _settings.update { it.copy(modelName = name.trim()) }
    }

    fun setConfidenceThreshold(threshold: Float) {
        _settings.update { it.copy(confidenceThreshold = threshold.coerceIn(0.05f, 0.99f)) }
    }

    fun setSimulatorParams(targetCount: Int, speedMult: Float, difficulty: Difficulty) {
        _settings.update {
            it.copy(
                targetCount = targetCount.coerceIn(1, 8),
                speedMultiplier = speedMult.coerceIn(0.5f, 5.0f),
                difficulty = difficulty
            )
        }
    }
}
