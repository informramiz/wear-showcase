package com.codebyramiz.breathefocus.wear.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.time.TimeChanges
import com.codebyramiz.breathefocus.core.data.time.today
import com.codebyramiz.breathefocus.core.model.MindfulStats
import com.codebyramiz.breathefocus.core.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val loading: Boolean = true,
    val todayMinutes: Int = 0,
    val streakDays: Int = 0,
    val recent: List<Session> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: SessionRepository,
    private val clock: Clock,
    timeChanges: TimeChanges,
) : ViewModel() {
    // The zone is read when the date changes, so a time-zone change regroups the sessions.

    val state: StateFlow<HistoryUiState> = combine(repository.observeAll(), clock.today(timeChanges.events)) {
            sessions,
            today,
        ->
        val stats = MindfulStats(clock.zone)
        HistoryUiState(
            loading = false,
            todayMinutes = stats.minutesOn(today, sessions),
            streakDays = stats.streak(today, sessions),
            recent = sessions.take(RECENT_LIMIT),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
