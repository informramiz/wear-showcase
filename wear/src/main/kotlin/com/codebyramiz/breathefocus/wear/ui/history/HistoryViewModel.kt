package com.codebyramiz.breathefocus.wear.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.model.MindfulStats
import com.codebyramiz.breathefocus.core.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val loading: Boolean = true,
    val todayMinutes: Int = 0,
    val streakDays: Int = 0,
    val recent: List<Session> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: SessionRepository,
    clock: Clock,
) : ViewModel() {
    private val stats = MindfulStats(clock.zone)

    val state: StateFlow<HistoryUiState> = repository.observeAll()
        .map { sessions ->
            val today = LocalDate.now(clock)
            HistoryUiState(
                loading = false,
                todayMinutes = stats.minutesOn(today, sessions),
                streakDays = stats.streak(today, sessions),
                recent = sessions.take(RECENT_LIMIT),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
