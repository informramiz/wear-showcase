package com.codebyramiz.breathefocus.wear.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.model.BreathState
import com.codebyramiz.breathefocus.core.model.BreathingEngine
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.haptics.Haptics
import com.codebyramiz.breathefocus.wear.health.HeartRate
import com.codebyramiz.breathefocus.wear.health.HeartRateMonitor
import com.codebyramiz.breathefocus.wear.session.SessionNotifier
import com.codebyramiz.breathefocus.wear.session.SessionRecorder
import com.codebyramiz.breathefocus.wear.session.SessionTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

data class BreathingUiState(
    val pattern: BreathingPattern = BreathingPattern.DEFAULT,
    val running: Boolean = false,
    val elapsedMillis: Long = 0,
    val breath: BreathState = BreathingEngine.stateAt(BreathingPattern.DEFAULT, 0),
    val heartRate: HeartRate = HeartRate.Unavailable("Not started"),
)

@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val ticker: SessionTicker,
    private val haptics: Haptics,
    private val heartRateMonitor: HeartRateMonitor,
    private val recorder: SessionRecorder,
    private val notifier: SessionNotifier,
    settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(BreathingUiState())
    val state: StateFlow<BreathingUiState> = _state.asStateFlow()

    /** Pattern the phone (or a previous run) picked: what Home pre-selects. */
    val defaultPattern: StateFlow<BreathingPattern> =
        settings.defaultPattern.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BreathingPattern.DEFAULT)

    private var sessionJob: Job? = null
    private var startedAtEpochMillis = 0L
    private val heartRateSamples = mutableListOf<Int>()

    fun start(pattern: BreathingPattern, startedAtElapsedRealtime: Long = 0L) {
        if (_state.value.running) return
        startedAtEpochMillis = clock.millis()
        heartRateSamples.clear()
        _state.value = BreathingUiState(
            pattern = pattern,
            running = true,
            breath = BreathingEngine.stateAt(pattern, 0),
            heartRate = HeartRate.Acquiring,
        )
        notifier.started(pattern.displayName + " breathing", startedAtElapsedRealtime)
        sessionJob = viewModelScope.launch {
            launch {
                heartRateMonitor.readings().collect { reading ->
                    if (reading is HeartRate.Bpm) heartRateSamples += reading.value
                    _state.update { it.copy(heartRate = reading) }
                }
            }
            var lastPhaseIndex = -1L
            ticker.elapsedMillis().collect { elapsed ->
                val phaseIndex = BreathingEngine.phaseIndexAt(pattern, elapsed)
                val breath = BreathingEngine.stateAt(pattern, elapsed)
                if (phaseIndex != lastPhaseIndex) {
                    lastPhaseIndex = phaseIndex
                    haptics.phaseChanged(breath.phase)
                }
                _state.update { it.copy(elapsedMillis = elapsed, breath = breath) }
            }
        }
    }

    fun stop() {
        val current = _state.value
        if (!current.running) return
        sessionJob?.cancel()
        sessionJob = null
        notifier.stopped()
        _state.update { it.copy(running = false) }
        val session = Session(
            id = UUID.randomUUID().toString(),
            type = SessionType.BREATHING,
            pattern = current.pattern,
            startedAtEpochMillis = startedAtEpochMillis,
            durationMillis = current.elapsedMillis,
            averageHeartRate = heartRateSamples.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        )
        viewModelScope.launch { recorder.record(session) }
    }

    /** Resolves the pattern for a tile/launcher quick start. */
    suspend fun resolveDefaultPattern(): BreathingPattern = defaultPattern.first()

    override fun onCleared() {
        // Leaving the screen mid-session still records it.
        if (_state.value.running) stop()
    }
}
