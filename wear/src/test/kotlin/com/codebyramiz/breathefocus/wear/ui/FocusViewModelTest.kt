package com.codebyramiz.breathefocus.wear.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codebyramiz.breathefocus.core.model.FocusPhase
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakeHaptics
import com.codebyramiz.breathefocus.wear.FakeNotifier
import com.codebyramiz.breathefocus.wear.FakeSessionRepository
import com.codebyramiz.breathefocus.wear.FakeSync
import com.codebyramiz.breathefocus.wear.FakeTicker
import com.codebyramiz.breathefocus.wear.MainDispatcherRule
import com.codebyramiz.breathefocus.wear.testRecorder
import com.codebyramiz.breathefocus.wear.ui.focus.FocusViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class FocusViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-13T09:00:00Z"), ZoneOffset.UTC)
    private val ticker = FakeTicker(main.dispatcher.scheduler)
    private val haptics = FakeHaptics()
    private val notifier = FakeNotifier()
    private val repository = FakeSessionRepository()
    private val store = ViewModelStore()

    // Lazy so the ViewModel is built after MainDispatcherRule has set the test Main dispatcher.
    private val vm by lazy {
        ViewModelProvider.create(
            store,
            viewModelFactory {
                initializer {
                    FocusViewModel(
                        ticker,
                        haptics,
                        testRecorder(repository, FakeSync(), clock, main.dispatcher),
                        notifier,
                        clock,
                    )
                }
            },
        )[FocusViewModel::class]
    }

    @After
    fun clear() = store.clear()

    @Test
    fun `buzzes when a focus block ends and when the break ends, with the screen off`() = runTest {
        vm.start()
        advanceTimeBy(24 * 60_000L)
        assertEquals(0, haptics.blocksFinished)
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(1, haptics.blocksFinished)
        advanceTimeBy(5 * 60_000L)
        runCurrent()
        assertEquals(2, haptics.blocksFinished)
        vm.stop()
    }

    @Test
    fun `state counts down while collected`() = runTest {
        vm.start()
        backgroundScope.launch { vm.state.collect {} }
        advanceTimeBy(25 * 60_000L + 100)
        assertEquals(FocusPhase.BREAK, vm.state.value.pomodoro.phase)
        vm.stop()
    }

    @Test
    fun `stop saves only focused minutes`() = runTest {
        vm.start()
        advanceTimeBy(28 * 60_000L) // 25 focus + 3 break
        vm.stop()

        val saved = repository.sessions.value.single()
        assertEquals(SessionType.FOCUS, saved.type)
        assertEquals(25 * 60_000L, saved.durationMillis)
        assertFalse(notifier.running)
        assertFalse(vm.state.value.running)
    }

    @Test
    fun `leaving the screen records the session`() = runTest {
        vm.start()
        advanceTimeBy(10 * 60_000L)
        store.clear()

        assertEquals(10 * 60_000L, repository.sessions.value.single().durationMillis)
        assertFalse(notifier.running)
    }
}
