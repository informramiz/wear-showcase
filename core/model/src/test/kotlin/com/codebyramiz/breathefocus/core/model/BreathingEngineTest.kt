package com.codebyramiz.breathefocus.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BreathingEngineTest {

    @Test
    fun `box pattern starts with inhale and an empty ring`() {
        val state = BreathingEngine.stateAt(BreathingPattern.BOX, 0)
        assertEquals(BreathPhase.INHALE, state.phase)
        assertEquals(0f, state.ringScale, 0.0001f)
        assertEquals(4_000L, state.phaseRemainingMillis)
        assertEquals(0, state.cycle)
    }

    @Test
    fun `box pattern walks through all four phases`() {
        val p = BreathingPattern.BOX
        assertEquals(BreathPhase.HOLD_IN, BreathingEngine.stateAt(p, 4_000).phase)
        assertEquals(BreathPhase.EXHALE, BreathingEngine.stateAt(p, 8_000).phase)
        assertEquals(BreathPhase.HOLD_OUT, BreathingEngine.stateAt(p, 12_000).phase)
        assertEquals(BreathPhase.INHALE, BreathingEngine.stateAt(p, 16_000).phase)
    }

    @Test
    fun `cycle counter increments every full cycle`() {
        assertEquals(1, BreathingEngine.stateAt(BreathingPattern.BOX, 16_000).cycle)
        assertEquals(2, BreathingEngine.stateAt(BreathingPattern.RELAX_478, 38_000).cycle)
    }

    @Test
    fun `478 pattern has no bottom hold`() {
        val p = BreathingPattern.RELAX_478
        assertEquals(19_000L, p.cycleMillis)
        assertEquals(BreathPhase.HOLD_IN, BreathingEngine.stateAt(p, 4_000).phase)
        assertEquals(BreathPhase.EXHALE, BreathingEngine.stateAt(p, 11_000).phase)
        assertEquals(BreathPhase.INHALE, BreathingEngine.stateAt(p, 18_999 + 1).phase)
    }

    @Test
    fun `ring is full at top hold and half way through inhale`() {
        val p = BreathingPattern.COHERENT
        assertEquals(0.5f, BreathingEngine.stateAt(p, 2_750).ringScale, 0.001f)
        assertEquals(1f, BreathingEngine.stateAt(BreathingPattern.BOX, 5_000).ringScale, 0.0001f)
        assertEquals(0f, BreathingEngine.stateAt(BreathingPattern.BOX, 13_000).ringScale, 0.0001f)
    }

    @Test
    fun `phase index counts every phase change across cycles`() {
        val p = BreathingPattern.COHERENT
        assertEquals(0L, BreathingEngine.phaseIndexAt(p, 0))
        assertEquals(1L, BreathingEngine.phaseIndexAt(p, 5_500))
        assertEquals(2L, BreathingEngine.phaseIndexAt(p, 11_000))
        assertEquals(5L, BreathingEngine.phaseIndexAt(p, 27_600))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative elapsed time is rejected`() {
        BreathingEngine.stateAt(BreathingPattern.BOX, -1)
    }

    @Test
    fun `unknown pattern name falls back to default`() {
        assertEquals(BreathingPattern.DEFAULT, BreathingPattern.fromNameOrDefault("nope"))
        assertEquals(BreathingPattern.COHERENT, BreathingPattern.fromNameOrDefault("COHERENT"))
    }
}
