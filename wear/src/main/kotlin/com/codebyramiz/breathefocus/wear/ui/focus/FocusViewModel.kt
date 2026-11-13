package com.codebyramiz.breathefocus.wear.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebyramiz.breathefocus.core.model.ActiveSession
import com.codebyramiz.breathefocus.core.model.Pomodoro
import com.codebyramiz.breathefocus.core.model.PomodoroConfig
import com.codebyramiz.breathefocus.core.model.PomodoroState
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.session.ActiveSessions
import com.codebyramiz.breathefocus.wear.session.PomodoroBoundaries
import com.codebyramiz.breathefocus.wear.session.SessionFrames
import com.codebyramiz.breathefocus.wear.session.SessionNotifier
import com.codebyramiz.breathefocus.wear.session.SessionTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
 * and records it. The running session is persisted, so a killed process continues it (system
 * restore) or records it (fresh launch). Block-end haptics come from wakeup alarms
 * ([PomodoroBoundaries]), which fire with the screen off and without this process; the
 * countdown ticks only while the UI collects [state].
 */
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val ticker: SessionTicker,
    private val activeSessions: ActiveSessions,
    private val boundaries: PomodoroBoundaries,
    private val notifier: SessionNotifier,
) : ViewModel() {

    private class Running(val id: String, val startedAt: Long, val startedAtEpochMillis: Long)

    private val config = PomodoroConfig()
    private val frames = SessionFrames(ticker)
    private val active = MutableStateFlow<Running?>(null)
    private val finished = MutableStateFlow(FocusUiState())
    private var startJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<FocusUiState> = active
        .flatMapLatest { session ->
            if (session == null) {
                finished
            } else {
                frames.frames.map { now ->
                    val elapsed = now - session.startedAt
                    FocusUiState(running = true, elapsedMillis = elapsed, pomodoro = Pomodoro.stateAt(config, elapsed))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    init {
        // A restored screen shows the session that was running when the process was killed.
        viewModelScope.launch { activeSessions.current(SessionType.FOCUS)?.let(::run) }
    }

    fun start() {
        if (active.value != null || startJob != null) return
        startJob = viewModelScope.launch { run(activeSessions.resumeOrBegin(SessionType.FOCUS, pattern = null)) }
    }

    private fun run(persisted: ActiveSession) {
        if (active.value != null) return
        val session = Running(persisted.id, persisted.startedAtElapsedRealtime, persisted.startedAtEpochMillis)
        active.value = session
        notifier.started("Focus", session.startedAt)
        boundaries.scheduleNext(persisted)
    }

    fun stop() {
        val session = active.value ?: return
        val elapsed = ticker.now() - session.startedAt
        notifier.stopped()
        finished.value =
            FocusUiState(running = false, elapsedMillis = elapsed, pomodoro = Pomodoro.stateAt(config, elapsed))
        active.value = null
        // Only focused time counts toward mindful minutes, not the breaks. finish() also cancels the alarm.
        activeSessions.finish(
            Session(
                id = session.id,
                type = SessionType.FOCUS,
                pattern = null,
                startedAtEpochMillis = session.startedAtEpochMillis,
                durationMillis = Pomodoro.focusedMillis(config, elapsed),
                averageHeartRate = null,
            ),
        )
    }

    fun setAmbient(isAmbient: Boolean) = frames.setAmbient(isAmbient)

    fun onAmbientTick() = frames.onAmbientTick()

    override fun onCleared() {
        // The nav entry was popped: end and record the session.
        stop()
    }
}
