package com.codebyramiz.breathefocus.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.data.time.today
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.DayTotal
import com.codebyramiz.breathefocus.core.model.MindfulStats
import com.codebyramiz.breathefocus.core.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val week: List<DayTotal> = emptyList(),
    val todayMinutes: Int = 0,
    val goalMinutes: Int = MindfulStats.DAILY_GOAL_MINUTES,
    val streakDays: Int = 0,
    val recent: List<Session> = emptyList(),
    val defaultPattern: BreathingPattern = BreathingPattern.DEFAULT,
    val watchSync: WatchSync = WatchSync.Idle,
) {
    val weekMinutes: Int get() = week.sumOf { it.minutes }
}

sealed interface WatchSync {
    data object Idle : WatchSync
    data object Sent : WatchSync
    data object Failed : WatchSync
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessions: SessionRepository,
    private val settings: SettingsRepository,
    private val sync: DataLayerSync,
    clock: Clock,
) : ViewModel() {

    private val stats = MindfulStats(clock.zone)
    private val watchSync = MutableStateFlow<WatchSync>(WatchSync.Idle)

    val state: StateFlow<HomeUiState> = combine(
        sessions.observeAll(),
        settings.defaultPattern,
        watchSync,
        clock.today(),
    ) { all, pattern, syncState, today ->
        HomeUiState(
            week = stats.lastSevenDays(today, all),
            todayMinutes = stats.minutesOn(today, all),
            streakDays = stats.streak(today, all),
            recent = all.sortedByDescending { it.startedAtEpochMillis }.take(RECENT_COUNT),
            defaultPattern = pattern,
            watchSync = syncState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    /** Saves locally first so the phone UI is right even when no watch is connected. */
    fun selectPattern(pattern: BreathingPattern) {
        viewModelScope.launch {
            settings.setDefaultPattern(pattern)
            watchSync.value = try {
                sync.publishDefaultPattern(pattern)
                WatchSync.Sent
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                WatchSync.Failed
            }
        }
    }

    private companion object {
        const val RECENT_COUNT = 8
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
