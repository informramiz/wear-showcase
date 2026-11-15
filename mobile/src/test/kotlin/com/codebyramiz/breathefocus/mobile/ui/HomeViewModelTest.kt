package com.codebyramiz.breathefocus.mobile.ui

import com.codebyramiz.breathefocus.core.data.sync.DataLayerCoordinator
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.mobile.FakeSessionRepository
import com.codebyramiz.breathefocus.mobile.FakeSettings
import com.codebyramiz.breathefocus.mobile.FakeSync
import com.codebyramiz.breathefocus.mobile.MainDispatcherRule
import com.codebyramiz.breathefocus.mobile.SchedulerClock
import com.codebyramiz.breathefocus.mobile.session
import com.codebyramiz.breathefocus.mobile.sync.SettingsPublisher
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log on the failed-send path.
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    // Friday 2026-11-13 12:00 UTC.
    private val now = Instant.parse("2026-11-13T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val day = 24 * 60 * 60_000L

    private val sessions = FakeSessionRepository()
    private val settings = FakeSettings()

    private fun publisher(sync: FakeSync) =
        SettingsPublisher(settings, sync, DataLayerCoordinator(), CoroutineScope(main.dispatcher))

    private fun viewModel(sync: FakeSync = FakeSync()) = HomeViewModel(sessions, settings, publisher(sync), clock)

    @Test
    fun `aggregates today, streak, week and recent from synced sessions`() = runTest {
        val t = now.toEpochMilli()
        sessions.sessions.value = listOf(
            session("a", t - 60_000L, minutes = 6, hr = 64),
            session("b", t - 30 * 60_000L, minutes = 5),
            session("c", t - day, minutes = 3),
            session("old", t - 10 * day, minutes = 20),
        )
        val vm = viewModel()
        backgroundScope.launch { vm.state.collect {} }

        val state = vm.state.first { it.week.isNotEmpty() }

        assertEquals(11, state.todayMinutes)
        assertEquals(2, state.streakDays)
        assertEquals(listOf(0, 0, 0, 0, 0, 3, 11), state.week.map { it.minutes })
        assertEquals(14, state.weekMinutes)
        assertEquals(listOf("a", "b", "c", "old"), state.recent.map { it.id })
    }

    @Test
    fun `selecting a pattern saves it and sends it to the watch`() = runTest {
        val sync = FakeSync()
        val vm = viewModel(sync)
        backgroundScope.launch { vm.state.collect {} }

        vm.selectPattern(BreathingPattern.COHERENT)

        assertEquals(BreathingPattern.COHERENT, settings.pattern.value)
        assertEquals(listOf(BreathingPattern.COHERENT), sync.patterns)
        val state = vm.state.first { it.watchSync == WatchSync.Sent }
        assertEquals(BreathingPattern.COHERENT, state.defaultPattern)
    }

    @Test
    fun `no watch connected keeps the local choice and reports it`() = runTest {
        val vm = viewModel(FakeSync(fail = true))
        backgroundScope.launch { vm.state.collect {} }

        vm.selectPattern(BreathingPattern.BOX)

        assertEquals(BreathingPattern.BOX, settings.pattern.value)
        assertEquals(BreathingPattern.BOX, settings.pending)
        assertEquals(WatchSync.Failed, vm.state.first { it.watchSync != WatchSync.Idle }.watchSync)
    }

    @Test
    fun `today rolls over at midnight while the screen stays open`() = runTest {
        val beforeMidnight = Instant.parse("2026-11-13T23:59:00Z")
        val movingClock = SchedulerClock(testScheduler, beforeMidnight)
        sessions.sessions.value = listOf(session("late", beforeMidnight.toEpochMilli() - 30 * 60_000L, minutes = 5))
        val vm = HomeViewModel(sessions, settings, publisher(FakeSync()), movingClock)
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        assertEquals(5, vm.state.value.todayMinutes)

        // No data changes; only the date does.
        advanceTimeBy(2 * 60_000L)
        runCurrent()

        assertEquals(0, vm.state.value.todayMinutes)
        assertEquals(listOf(5, 0), vm.state.value.week.takeLast(2).map { it.minutes })
    }
}
