package com.codebyramiz.breathefocus.wear.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebyramiz.breathefocus.core.model.ActiveSession
import com.codebyramiz.breathefocus.core.model.BreathState
import com.codebyramiz.breathefocus.core.model.BreathingEngine
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.haptics.Haptics
import com.codebyramiz.breathefocus.wear.health.HeartRate
import com.codebyramiz.breathefocus.wear.health.HeartRateMonitor
import com.codebyramiz.breathefocus.wear.session.ActiveSessions
import com.codebyramiz.breathefocus.wear.session.SessionFrames
import com.codebyramiz.breathefocus.wear.session.SessionNotifier
import com.codebyramiz.breathefocus.wear.session.SessionTicker
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * the screen clears the ViewModel, which ends and records the session. The running session is
 * also persisted, so if the process is killed it is continued (system restore) or recorded
 * (fresh launch) instead of being lost.
 *
 * Phase haptics keep running while the screen is off: the foreground service holds a bounded
 * wake lock so the delays below are not stalled by deep sleep. A session therefore ends on its own
 * after [MAX_SESSION_MILLIS]. The ring animation and heart-rate
 * measurement run only while the UI collects [state].
 */
@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val ticker: SessionTicker,
    private val haptics: Haptics,
    private val heartRateMonitor: HeartRateMonitor,
    private val activeSessions: ActiveSessions,
    private val notifier: SessionNotifier,
) : ViewModel() {

    private class Running(
        val id: String,
        val pattern: BreathingPattern,
        val startedAt: Long,
        val startedAtEpochMillis: Long,
    )

    private val frames = SessionFrames(ticker)
    private val active = MutableStateFlow<Running?>(null)
    private val finished = MutableStateFlow(BreathingUiState())
    private val heartRateSamples = mutableListOf<Int>()
    private var startJob: Job? = null
    private var hapticsJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<BreathingUiState> = active
        .flatMapLatest { session -> if (session == null) finished else liveState(session) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BreathingUiState())

    /** Begins a session, or continues the one that was running when the process was killed. */
    fun start(pattern: BreathingPattern) {
        if (active.value != null || startJob != null) return
        startJob = viewModelScope.launch {
            val persisted = activeSessions.resumeOrBegin(SessionType.BREATHING, pattern)
            run(persisted)
        }
    }

    private fun run(persisted: ActiveSession) {
        val pattern = persisted.pattern ?: BreathingPattern.DEFAULT
        val session = Running(
            id = persisted.id,
            pattern = pattern,
            startedAt = persisted.startedAtElapsedRealtime,
            startedAtEpochMillis = persisted.startedAtEpochMillis,
        )
        heartRateSamples.clear()
        active.value = session
        notifier.started(pattern.displayName + " breathing", session.startedAt, keepAwake = true)
        hapticsJob = viewModelScope.launch { phaseHaptics(session) }
    }

    fun stop() {
        val session = active.value ?: return
        val elapsed = (ticker.now() - session.startedAt).coerceAtMost(MAX_SESSION_MILLIS)
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
        activeSessions.finish(
            Session(
                id = session.id,
                type = SessionType.BREATHING,
                pattern = session.pattern,
                startedAtEpochMillis = session.startedAtEpochMillis,
                durationMillis = elapsed,
                averageHeartRate = heartRateSamples.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            ),
        )
    }

    fun setAmbient(isAmbient: Boolean) = frames.setAmbient(isAmbient)

    fun onAmbientTick() = frames.onAmbientTick()

    private fun liveState(session: Running): Flow<BreathingUiState> = combine(
        frames.frames,
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
    private suspend fun phaseHaptics(session: Running) {
        var lastPhaseIndex = -1L
        while (true) {
            val elapsed = ticker.now() - session.startedAt
            if (elapsed >= MAX_SESSION_MILLIS) {
                // The service's wake lock has expired, so cues could no longer be on time.
                stop()
                return
            }
            val phaseIndex = BreathingEngine.phaseIndexAt(session.pattern, elapsed)
            val breath = BreathingEngine.stateAt(session.pattern, elapsed)
            if (phaseIndex != lastPhaseIndex) {
                lastPhaseIndex = phaseIndex
                haptics.phaseChanged(breath.phase)
            }
            delay(breath.phaseRemainingMillis.coerceAtLeast(1))
        }
    }

    companion object {
        /** Longest a breathing session runs; matches the bounded wake lock that keeps its cues on time. */
        const val MAX_SESSION_MILLIS = ActiveSessions.MAX_BREATHING_MILLIS
    }

    override fun onCleared() {
        // The nav entry was popped (swipe back or stop): end and record the session.
        stop()
    }
}
