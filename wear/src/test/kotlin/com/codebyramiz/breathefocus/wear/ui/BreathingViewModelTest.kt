package com.codebyramiz.breathefocus.wear.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codebyramiz.breathefocus.core.model.BreathPhase
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakeHaptics
import com.codebyramiz.breathefocus.wear.FakeHeartRate
import com.codebyramiz.breathefocus.wear.FakeNotifier
import com.codebyramiz.breathefocus.wear.FakeSessionRepository
import com.codebyramiz.breathefocus.wear.FakeSync
import com.codebyramiz.breathefocus.wear.FakeTicker
import com.codebyramiz.breathefocus.wear.MainDispatcherRule
import com.codebyramiz.breathefocus.wear.health.HeartRate
import com.codebyramiz.breathefocus.wear.testRecorder
import com.codebyramiz.breathefocus.wear.ui.breathing.BreathingViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BreathingViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-13T09:00:00Z"), ZoneOffset.UTC)
    private val ticker = FakeTicker(main.dispatcher.scheduler)
    private val haptics = FakeHaptics()
    private val heartRate = FakeHeartRate()
    private val notifier = FakeNotifier()
    private val repository = FakeSessionRepository()
    private val sync = FakeSync()
    private var refreshes = 0
    private val recorder = testRecorder(repository, sync, clock, main.dispatcher) { refreshes++ }
    private val stores = mutableListOf<ViewModelStore>()

    /** Creates the ViewModel the way a nav entry does, so clearing [store] runs onCleared. */
    private fun viewModel(store: ViewModelStore = ViewModelStore().also { stores += it }): BreathingViewModel {
        val factory = viewModelFactory {
            initializer { BreathingViewModel(ticker, haptics, heartRate, recorder, notifier, clock) }
        }
        return ViewModelProvider.create(store, factory)[BreathingViewModel::class]
    }

    private fun TestScope.collect(vm: BreathingViewModel) {
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
    }

    @After
    fun clearStores() = stores.forEach { it.clear() }

    @Test
    fun `start shows the first phase, posts the ongoing activity and buzzes once`() = runTest {
        val vm = viewModel()
        vm.start(BreathingPattern.BOX)
        collect(vm)

        assertTrue(vm.state.value.running)
        assertEquals(BreathPhase.INHALE, vm.state.value.breath.phase)
        assertTrue(notifier.running)
        assertEquals(listOf(BreathPhase.INHALE), haptics.phases)
        vm.stop()
    }

    @Test
    fun `haptic fires once per phase change, also with the screen off`() = runTest {
        val vm = viewModel()
        vm.start(BreathingPattern.BOX)
        // Nothing collects state here, as when the screen is off.
        advanceTimeBy(16_000)
        runCurrent()

        assertEquals(
            listOf(
                BreathPhase.INHALE,
                BreathPhase.HOLD_IN,
                BreathPhase.EXHALE,
                BreathPhase.HOLD_OUT,
                BreathPhase.INHALE,
            ),
            haptics.phases,
        )
        vm.stop()
    }

    @Test
    fun `heart rate readings show live and their average is saved`() = runTest {
        val vm = viewModel()
        vm.start(BreathingPattern.COHERENT)
        collect(vm)
        heartRate.readings.emit(HeartRate.Bpm(60))
        runCurrent()
        assertEquals(HeartRate.Bpm(60), vm.state.value.heartRate)
        heartRate.readings.emit(HeartRate.Bpm(70))
        advanceTimeBy(65_000)

        vm.stop()

        val saved = repository.sessions.value.single()
        assertEquals(SessionType.BREATHING, saved.type)
        assertEquals(BreathingPattern.COHERENT, saved.pattern)
        assertEquals(65_000, saved.durationMillis)
        assertEquals(65, saved.averageHeartRate)
        assertEquals(clock.millis(), saved.startedAtEpochMillis)
        assertFalse(notifier.running)
        assertFalse(vm.state.value.running)
        assertEquals(1, refreshes)
        assertEquals(listOf(listOf(saved)), sync.published)
    }

    @Test
    fun `unavailable heart rate still records the session without bpm`() = runTest {
        val vm = viewModel()
        vm.start(BreathingPattern.BOX)
        collect(vm)
        heartRate.readings.emit(HeartRate.Unavailable("No sensor"))
        advanceTimeBy(90_000)
        vm.stop()

        assertEquals(null, repository.sessions.value.single().averageHeartRate)
    }

    @Test
    fun `sessions shorter than a minute are discarded`() = runTest {
        val vm = viewModel()
        vm.start(BreathingPattern.BOX)
        advanceTimeBy(59_000)
        vm.stop()

        assertTrue(repository.sessions.value.isEmpty())
        assertEquals(0, refreshes)
    }

    @Test
    fun `leaving the screen mid-session records it, and the next visit starts a fresh session`() = runTest {
        val firstVisit = ViewModelStore()
        viewModel(firstVisit).start(BreathingPattern.BOX)
        advanceTimeBy(70_000)

        // Swiping back pops the nav entry, which clears its ViewModel.
        firstVisit.clear()

        val saved = repository.sessions.value.single()
        assertEquals(BreathingPattern.BOX, saved.pattern)
        assertEquals(70_000, saved.durationMillis)
        assertFalse(notifier.running)

        val vm = viewModel()
        vm.start(BreathingPattern.RELAX_478)
        collect(vm)

        assertEquals(BreathingPattern.RELAX_478, vm.state.value.pattern)
        assertEquals(0, vm.state.value.elapsedMillis)
        assertEquals("${BreathingPattern.RELAX_478.displayName} breathing", notifier.lastTitle)
        assertTrue(notifier.running)
        // End the session so runTest's final drain doesn't run the haptics loop forever.
        vm.stop()
    }
}
