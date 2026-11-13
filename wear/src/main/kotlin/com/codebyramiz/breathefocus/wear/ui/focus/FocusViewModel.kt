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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusUiState(
    val running: Boolean = false,
    val elapsedMillis: Long = 0,
    val pomodoro: PomodoroState = Pomodoro.stateAt(PomodoroConfig(), 0),
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val ticker: SessionTicker,
    private val haptics: Haptics,
    private val recorder: SessionRecorder,
    private val notifier: SessionNotifier,
    private val clock: Clock,
) : ViewModel() {

    private val config = PomodoroConfig()
    private val _state = MutableStateFlow(FocusUiState())
    val state: StateFlow<FocusUiState> = _state.asStateFlow()

    private var job: Job? = null
    private var startedAtEpochMillis = 0L

    fun start(startedAtElapsedRealtime: Long = 0L) {
        if (_state.value.running) return
        startedAtEpochMillis = clock.millis()
        _state.value = FocusUiState(running = true)
        notifier.started("Focus", startedAtElapsedRealtime)
        job = viewModelScope.launch {
            var lastPhase = FocusPhase.FOCUS
            ticker.elapsedMillis().collect { elapsed ->
                val pomodoro = Pomodoro.stateAt(config, elapsed)
                if (pomodoro.phase != lastPhase) {
                    lastPhase = pomodoro.phase
                    haptics.focusBlockFinished()
                }
                _state.update { it.copy(elapsedMillis = elapsed, pomodoro = pomodoro) }
            }
        }
    }

    fun stop() {
        val current = _state.value
        if (!current.running) return
        job?.cancel()
        job = null
        notifier.stopped()
        _state.update { it.copy(running = false) }
        // Only focused time counts toward mindful minutes, not the breaks.
        val focused = Pomodoro.focusedMillis(config, current.elapsedMillis)
        val session = Session(
            id = UUID.randomUUID().toString(),
            type = SessionType.FOCUS,
            pattern = null,
            startedAtEpochMillis = startedAtEpochMillis,
            durationMillis = focused,
            averageHeartRate = null,
        )
        viewModelScope.launch { recorder.record(session) }
    }

    override fun onCleared() {
        if (_state.value.running) stop()
    }
}
