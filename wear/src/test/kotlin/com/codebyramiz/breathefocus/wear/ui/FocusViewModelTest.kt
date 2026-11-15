package com.codebyramiz.breathefocus.wear.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codebyramiz.breathefocus.core.model.ActiveSession
import com.codebyramiz.breathefocus.core.model.FocusPhase
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakeHaptics
import com.codebyramiz.breathefocus.wear.FakeNotifier
import com.codebyramiz.breathefocus.wear.FakePhaseAlarms
import com.codebyramiz.breathefocus.wear.FakeSessionStore
import com.codebyramiz.breathefocus.wear.FakeSync
import com.codebyramiz.breathefocus.wear.FakeTicker
import com.codebyramiz.breathefocus.wear.MainDispatcherRule
import com.codebyramiz.breathefocus.wear.session.PomodoroBoundaries
import com.codebyramiz.breathefocus.wear.testActiveSessions
import com.codebyramiz.breathefocus.wear.ui.focus.FocusViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class FocusViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-13T09:00:00Z"), ZoneOffset.UTC)
    private val ticker = FakeTicker(main.dispatcher.scheduler)
    private val haptics = FakeHaptics()
    private val notifier = FakeNotifier()
    private val repository = FakeSessionStore()
    private val alarms = FakePhaseAlarms()
    private val boundaries = PomodoroBoundaries(repository, alarms, haptics, ticker)
    private val store = ViewModelStore()

    // Lazy so the ViewModel is built after MainDispatcherRule has set the test Main dispatcher.
    private val vm by lazy { newViewModel(store) }

    private fun newViewModel(store: ViewModelStore): FocusViewModel {
        val activeSessions =
            testActiveSessions(repository, FakeSync(), alarms, ticker, clock, CoroutineScope(main.dispatcher))
        return ViewModelProvider.create(
            store,
            viewModelFactory { initializer { FocusViewModel(ticker, activeSessions, boundaries, notifier) } },
        )[FocusViewModel::class]
    }

    @After
    fun clear() = store.clear()

    @Test
    fun `arms an alarm for the end of the block instead of delaying in the process`() = runTest {
        vm.start()

        assertEquals(25 * 60_000L, alarms.triggerAt)
    }

    @Test
    fun `an alarm buzzes at the block end and arms the next boundary`() = runTest {
        vm.start()

        advanceTimeBy(25 * 60_000L)
        boundaries.onAlarm()
        assertEquals(1, haptics.blocksFinished)
        assertEquals(30 * 60_000L, alarms.triggerAt)

        advanceTimeBy(5 * 60_000L)
        boundaries.onAlarm()
        assertEquals(2, haptics.blocksFinished)
        assertEquals(55 * 60_000L, alarms.triggerAt)
    }

    @Test
    fun `an alarm with no focus session running does nothing`() = runTest {
        boundaries.onAlarm()

        assertEquals(0, haptics.blocksFinished)
        assertNull(alarms.triggerAt)
    }

    @Test
    fun `state counts down while collected`() = runTest {
        vm.start()
        backgroundScope.launch { vm.state.collect {} }
        advanceTimeBy(25 * 60_000L + 100)
        assertEquals(FocusPhase.BREAK, vm.state.value.pomodoro.phase)
    }

    @Test
    fun `stop saves only focused minutes and cancels the alarm`() = runTest {
        vm.start()
        advanceTimeBy(28 * 60_000L) // 25 focus + 3 break
        vm.stop()

        val saved = repository.sessions.value.single()
        assertEquals(SessionType.FOCUS, saved.type)
        assertEquals(25 * 60_000L, saved.durationMillis)
        assertFalse(notifier.running)
        assertFalse(vm.state.value.running)
        assertNull(alarms.triggerAt)
        assertNull(repository.active)
    }

    @Test
    fun `leaving the screen records the session`() = runTest {
        vm.start()
        advanceTimeBy(10 * 60_000L)
        store.clear()

        assertEquals(10 * 60_000L, repository.sessions.value.single().durationMillis)
        assertFalse(notifier.running)
    }

    @Test
    fun `a restored screen shows the session that survived the process`() = runTest {
        repository.begin(ActiveSession("survivor", SessionType.FOCUS, null, 0, clock.millis()))
        advanceTimeBy(12 * 60_000L)

        backgroundScope.launch { vm.state.collect {} }
        runCurrent()

        assertEquals(true, vm.state.value.running)
        vm.stop()
        val saved = repository.sessions.value.single()
        assertEquals("survivor", saved.id)
        assertEquals(12 * 60_000L, saved.durationMillis)
    }
}
