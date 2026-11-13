package com.codebyramiz.breathefocus.mobile.ui

import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.mobile.FakeSessionRepository
import com.codebyramiz.breathefocus.mobile.FakeSettings
import com.codebyramiz.breathefocus.mobile.FakeSync
import com.codebyramiz.breathefocus.mobile.MainDispatcherRule
import com.codebyramiz.breathefocus.mobile.session
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    // Friday 2026-11-13 12:00 UTC.
    private val now = Instant.parse("2026-11-13T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val day = 24 * 60 * 60_000L

    private val sessions = FakeSessionRepository()
    private val settings = FakeSettings()

    private fun viewModel(sync: FakeSync = FakeSync()) = HomeViewModel(sessions, settings, sync, clock)

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
        assertEquals(WatchSync.Failed, vm.state.first { it.watchSync != WatchSync.Idle }.watchSync)
    }
}
