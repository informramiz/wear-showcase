package com.codebyramiz.breathefocus.wear.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebyramiz.breathefocus.core.model.FocusPhase
import com.codebyramiz.breathefocus.core.model.Pomodoro
import com.codebyramiz.breathefocus.core.model.PomodoroConfig
import com.codebyramiz.breathefocus.core.model.PomodoroState
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.haptics.Haptics
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusUiState(
    val running: Boolean = false,
    val elapsedMillis: Long = 0,
    val pomodoro: PomodoroState = Pomodoro.stateAt(PomodoroConfig(), 0),
)

/**
 * Runs one Pomodoro session for as long as its nav entry lives; clearing the ViewModel ends
 * and records it. Block-end haptics are scheduled per phase; the countdown ticks only while
 * the UI collects [state].
 */
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val ticker: SessionTicker,
    private val haptics: Haptics,
    private val recorder: SessionRecorder,
    private val notifier: SessionNotifier,
    private val clock: Clock,
) : ViewModel() {

    private class Active(val startedAt: Long, val startedAtEpochMillis: Long)

    private val config = PomodoroConfig()
    private val active = MutableStateFlow<Active?>(null)
    private val finished = MutableStateFlow(FocusUiState())
    private var hapticsJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<FocusUiState> = active
        .flatMapLatest { session ->
            if (session == null) {
                finished
            } else {
                ticker.frames().map { now ->
                    val elapsed = now - session.startedAt
                    FocusUiState(running = true, elapsedMillis = elapsed, pomodoro = Pomodoro.stateAt(config, elapsed))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    fun start() {
        if (active.value != null) return
        val session = Active(startedAt = ticker.now(), startedAtEpochMillis = clock.millis())
        active.value = session
        notifier.started("Focus", session.startedAt)
        hapticsJob = viewModelScope.launch { blockHaptics(session) }
    }

    fun stop() {
        val session = active.value ?: return
        val elapsed = ticker.now() - session.startedAt
        hapticsJob?.cancel()
        hapticsJob = null
        notifier.stopped()
        finished.value =
            FocusUiState(running = false, elapsedMillis = elapsed, pomodoro = Pomodoro.stateAt(config, elapsed))
        active.value = null
        // Only focused time counts toward mindful minutes, not the breaks.
        recorder.finish(
            Session(
                id = UUID.randomUUID().toString(),
                type = SessionType.FOCUS,
                pattern = null,
                startedAtEpochMillis = session.startedAtEpochMillis,
                durationMillis = Pomodoro.focusedMillis(config, elapsed),
                averageHeartRate = null,
            ),
        )
    }

    /** Buzzes when a focus block or a break ends, sleeping until then instead of polling. */
    private suspend fun blockHaptics(session: Active) {
        var lastPhase = FocusPhase.FOCUS
        while (true) {
            val pomodoro = Pomodoro.stateAt(config, ticker.now() - session.startedAt)
            if (pomodoro.phase != lastPhase) {
                lastPhase = pomodoro.phase
                haptics.focusBlockFinished()
            }
            delay(pomodoro.remainingMillis.coerceAtLeast(1))
        }
    }

    override fun onCleared() {
        // The nav entry was popped: end and record the session.
        stop()
    }
}
