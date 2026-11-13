package com.codebyramiz.breathefocus.wear.ui

import com.codebyramiz.breathefocus.core.model.FocusPhase
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakeHaptics
import com.codebyramiz.breathefocus.wear.FakeNotifier
import com.codebyramiz.breathefocus.wear.FakeSessionRepository
import com.codebyramiz.breathefocus.wear.FakeSync
import com.codebyramiz.breathefocus.wear.FakeTicker
import com.codebyramiz.breathefocus.wear.MainDispatcherRule
import com.codebyramiz.breathefocus.wear.session.SessionRecorder
import com.codebyramiz.breathefocus.wear.ui.focus.FocusViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class FocusViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-13T09:00:00Z"), ZoneOffset.UTC)
    private val ticker = FakeTicker()
    private val haptics = FakeHaptics()
    private val notifier = FakeNotifier()
    private val repository = FakeSessionRepository()
    private val vm =
        FocusViewModel(ticker, haptics, SessionRecorder(repository, FakeSync(), {}, clock), notifier, clock)

    @Test
    fun `buzzes when a focus block ends and when the break ends`() = runTest {
        vm.start()
        ticker.elapsed.emit(0)
        ticker.elapsed.emit(24 * 60_000L)
        assertEquals(0, haptics.blocksFinished)
        ticker.elapsed.emit(25 * 60_000L)
        assertEquals(FocusPhase.BREAK, vm.state.value.pomodoro.phase)
        assertEquals(1, haptics.blocksFinished)
        ticker.elapsed.emit(30 * 60_000L)
        assertEquals(2, haptics.blocksFinished)
    }

    @Test
    fun `stop saves only focused minutes`() = runTest {
        vm.start()
        ticker.elapsed.emit(28 * 60_000L) // 25 focus + 3 break
        vm.stop()

        val saved = repository.sessions.value.single()
        assertEquals(SessionType.FOCUS, saved.type)
        assertEquals(25 * 60_000L, saved.durationMillis)
        assertFalse(notifier.running)
        assertFalse(vm.state.value.running)
    }
}
