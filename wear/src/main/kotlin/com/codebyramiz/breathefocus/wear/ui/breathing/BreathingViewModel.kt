package com.codebyramiz.breathefocus.wear.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BreathingUiState(
    val pattern: BreathingPattern = BreathingPattern.DEFAULT,
    val running: Boolean = false,
    val elapsedMillis: Long = 0,
    val breath: BreathState = BreathingEngine.stateAt(BreathingPattern.DEFAULT, 0),
    val heartRate: HeartRate = HeartRate.Unavailable("Not started"),
)

/**
 * Runs one breathing session. The session lives as long as this ViewModel's nav entry: leaving
 * the screen clears the ViewModel, which ends and records the session.
 *
 * Phase haptics keep running while the screen is off, scheduled once per phase. The ring
 * animation and heart-rate measurement run only while the UI collects [state].
 */
@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val ticker: SessionTicker,
    private val haptics: Haptics,
    private val heartRateMonitor: HeartRateMonitor,
    private val recorder: SessionRecorder,
    private val notifier: SessionNotifier,
    private val clock: Clock,
) : ViewModel() {

    private class Active(val pattern: BreathingPattern, val startedAt: Long, val startedAtEpochMillis: Long)

    private val active = MutableStateFlow<Active?>(null)
    private val finished = MutableStateFlow(BreathingUiState())
    private val heartRateSamples = mutableListOf<Int>()
    private var hapticsJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<BreathingUiState> = active
        .flatMapLatest { session -> if (session == null) finished else liveState(session) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BreathingUiState())

    fun start(pattern: BreathingPattern) {
        if (active.value != null) return
        val session = Active(pattern, startedAt = ticker.now(), startedAtEpochMillis = clock.millis())
        heartRateSamples.clear()
        active.value = session
        notifier.started(pattern.displayName + " breathing", session.startedAt)
        hapticsJob = viewModelScope.launch { phaseHaptics(session) }
    }

    fun stop() {
        val session = active.value ?: return
        val elapsed = ticker.now() - session.startedAt
        hapticsJob?.cancel()
        hapticsJob = null
        notifier.stopped()
        finished.value = BreathingUiState(
            pattern = session.pattern,
            running = false,
            elapsedMillis = elapsed,
            breath = BreathingEngine.stateAt(session.pattern, elapsed),
        )
        active.value = null
        recorder.finish(
            Session(
                id = UUID.randomUUID().toString(),
                type = SessionType.BREATHING,
                pattern = session.pattern,
                startedAtEpochMillis = session.startedAtEpochMillis,
                durationMillis = elapsed,
                averageHeartRate = heartRateSamples.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            ),
        )
    }

    private fun liveState(session: Active): Flow<BreathingUiState> = combine(
        ticker.frames(),
        heartRateMonitor.readings()
            .onEach { if (it is HeartRate.Bpm) heartRateSamples += it.value }
            .onStart { emit(HeartRate.Acquiring) },
    ) { now, heartRate ->
        val elapsed = now - session.startedAt
        BreathingUiState(
            pattern = session.pattern,
            running = true,
            elapsedMillis = elapsed,
            breath = BreathingEngine.stateAt(session.pattern, elapsed),
            heartRate = heartRate,
        )
    }

    /** Buzzes at each phase change, sleeping until the next one instead of polling. */
    private suspend fun phaseHaptics(session: Active) {
        var lastPhaseIndex = -1L
        while (true) {
            val elapsed = ticker.now() - session.startedAt
            val phaseIndex = BreathingEngine.phaseIndexAt(session.pattern, elapsed)
            val breath = BreathingEngine.stateAt(session.pattern, elapsed)
            if (phaseIndex != lastPhaseIndex) {
                lastPhaseIndex = phaseIndex
                haptics.phaseChanged(breath.phase)
            }
            delay(breath.phaseRemainingMillis.coerceAtLeast(1))
        }
    }

    override fun onCleared() {
        // The nav entry was popped (swipe back or stop): end and record the session.
        stop()
    }
}
