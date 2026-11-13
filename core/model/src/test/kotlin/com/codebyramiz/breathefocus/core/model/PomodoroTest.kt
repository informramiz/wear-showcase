package com.codebyramiz.breathefocus.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroTest {
    private val config = PomodoroConfig(focusMillis = 25 * 60_000L, breakMillis = 5 * 60_000L)

    @Test
    fun `starts in focus with full time remaining`() {
        val s = Pomodoro.stateAt(config, 0)
        assertEquals(FocusPhase.FOCUS, s.phase)
        assertEquals(25 * 60_000L, s.remainingMillis)
        assertEquals(0, s.completedFocusBlocks)
    }

    @Test
    fun `switches to break after focus block and counts it`() {
        val s = Pomodoro.stateAt(config, 26 * 60_000L)
        assertEquals(FocusPhase.BREAK, s.phase)
        assertEquals(4 * 60_000L, s.remainingMillis)
        assertEquals(1, s.completedFocusBlocks)
    }

    @Test
    fun `second round starts focus again`() {
        val s = Pomodoro.stateAt(config, 30 * 60_000L)
        assertEquals(FocusPhase.FOCUS, s.phase)
        assertEquals(1, s.completedFocusBlocks)
        assertEquals(0f, s.progress, 0.0001f)
    }

    @Test
    fun `focused time excludes breaks`() {
        assertEquals(10 * 60_000L, Pomodoro.focusedMillis(config, 10 * 60_000L))
        assertEquals(25 * 60_000L, Pomodoro.focusedMillis(config, 29 * 60_000L))
        assertEquals(27 * 60_000L, Pomodoro.focusedMillis(config, 32 * 60_000L))
    }
}
