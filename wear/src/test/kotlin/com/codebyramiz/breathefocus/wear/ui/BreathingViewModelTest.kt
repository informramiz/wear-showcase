package com.codebyramiz.breathefocus.wear.ui

import com.codebyramiz.breathefocus.core.model.BreathPhase
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakeHaptics
import com.codebyramiz.breathefocus.wear.FakeHeartRate
import com.codebyramiz.breathefocus.wear.FakeNotifier
import com.codebyramiz.breathefocus.wear.FakeSessionRepository
import com.codebyramiz.breathefocus.wear.FakeSettings
import com.codebyramiz.breathefocus.wear.FakeSync
import com.codebyramiz.breathefocus.wear.FakeTicker
import com.codebyramiz.breathefocus.wear.MainDispatcherRule
import com.codebyramiz.breathefocus.wear.health.HeartRate
import com.codebyramiz.breathefocus.wear.session.SessionRecorder
import com.codebyramiz.breathefocus.wear.ui.breathing.BreathingViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BreathingViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-13T09:00:00Z"), ZoneOffset.UTC)
    private val ticker = FakeTicker()
    private val haptics = FakeHaptics()
    private val heartRate = FakeHeartRate()
    private val notifier = FakeNotifier()
    private val repository = FakeSessionRepository()
    private val sync = FakeSync()
    private var refreshes = 0
    private val recorder = SessionRecorder(repository, sync, { refreshes++ }, clock)

    private fun viewModel(settings: FakeSettings = FakeSettings()) =
        BreathingViewModel(ticker, haptics, heartRate, recorder, notifier, settings, clock)

    @Test
    fun `start shows the first phase, posts the ongoing activity and buzzes once`() = runTest {
        val vm = viewModel()
        vm.start(BreathingPattern.BOX)
        ticker.elapsed.emit(0)

        assertTrue(vm.state.value.running)
        assertEquals(BreathPhase.INHALE, vm.state.value.breath.phase)
        assertTrue(notifier.running)
        assertEquals(listOf(BreathPhase.INHALE), haptics.phases)
    }

    @Test
    fun `haptic fires once per phase change, not per tick`() = runTest {
        val vm = viewModel()
        vm.start(BreathingPattern.BOX)
        listOf(0L, 1_000, 3_999, 4_000, 6_000, 8_000, 12_000, 16_000).forEach { ticker.elapsed.emit(it) }

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
        assertEquals(16_000, vm.state.value.elapsedMillis)
    }

    @Test
    fun `heart rate readings show live and their average is saved`() = runTest {
        val vm = viewModel()
        vm.start(BreathingPattern.COHERENT)
        heartRate.readings.emit(HeartRate.Bpm(60))
        assertEquals(HeartRate.Bpm(60), vm.state.value.heartRate)
        heartRate.readings.emit(HeartRate.Bpm(70))
        ticker.elapsed.emit(65_000)

        vm.stop()

        val saved = repository.sessions.value.single()
        assertEquals(SessionType.BREATHING, saved.type)
        assertEquals(BreathingPattern.COHERENT, saved.pattern)
        assertEquals(65_000, saved.durationMillis)
        assertEquals(65, saved.averageHeartRate)
        assertEquals(clock.millis(), saved.startedAtEpochMillis)
        assertFalse(notifier.running)
        assertEquals(1, refreshes)
        assertEquals(listOf(listOf(saved)), sync.published)
    }

    @Test
    fun `unavailable heart rate still records the session without bpm`() = runTest {
        val vm = viewModel()
        vm.start(BreathingPattern.BOX)
        heartRate.readings.emit(HeartRate.Unavailable("No sensor"))
        ticker.elapsed.emit(30_000)
        vm.stop()

        assertEquals(null, repository.sessions.value.single().averageHeartRate)
    }

    @Test
    fun `sessions shorter than ten seconds are discarded`() = runTest {
        val vm = viewModel()
        vm.start(BreathingPattern.BOX)
        ticker.elapsed.emit(9_000)
        vm.stop()

        assertTrue(repository.sessions.value.isEmpty())
        assertEquals(0, refreshes)
    }

    @Test
    fun `default pattern follows settings synced from the phone`() = runTest {
        val vm = viewModel(FakeSettings(BreathingPattern.RELAX_478))
        assertEquals(BreathingPattern.RELAX_478, vm.resolveDefaultPattern())
    }
}
