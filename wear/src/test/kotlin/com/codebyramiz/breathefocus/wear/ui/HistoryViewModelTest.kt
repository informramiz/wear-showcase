package com.codebyramiz.breathefocus.wear.ui

import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakeSessionStore
import com.codebyramiz.breathefocus.wear.MainDispatcherRule
import com.codebyramiz.breathefocus.wear.SchedulerClock
import com.codebyramiz.breathefocus.wear.ui.history.HistoryViewModel
import java.time.Instant
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `today rolls over at midnight while the screen stays open`() = runTest {
        val beforeMidnight = Instant.parse("2026-11-13T23:59:00Z")
        val repository = FakeSessionStore()
        repository.sessions.value = listOf(
            Session(
                "late",
                SessionType.BREATHING,
                BreathingPattern.BOX,
                beforeMidnight.toEpochMilli() - 1_800_000,
                300_000,
                null,
            ),
        )
        val vm = HistoryViewModel(repository, SchedulerClock(testScheduler, beforeMidnight))
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        assertEquals(5, vm.state.value.todayMinutes)

        // No data changes; only the date does.
        advanceTimeBy(2 * 60_000L)
        runCurrent()

        assertEquals(0, vm.state.value.todayMinutes)
    }
}
