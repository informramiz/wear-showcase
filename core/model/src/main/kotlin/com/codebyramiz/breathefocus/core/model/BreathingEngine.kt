package com.codebyramiz.breathefocus.core.model

/**
 * Where a breathing session is at a point in time.
 *
 * @property phaseProgress 0..1 progress through the current phase.
 * @property ringScale 0..1 size of the breathing ring: grows on inhale, stays full on the
 * top hold, shrinks on exhale and stays empty on the bottom hold.
 */
data class BreathState(
    val phase: BreathPhase,
    val phaseProgress: Float,
    val phaseRemainingMillis: Long,
    val cycle: Int,
    val ringScale: Float,
)

/** Pure timing logic: maps elapsed time to a [BreathState]. No clocks, no Android. */
object BreathingEngine {

    fun stateAt(pattern: BreathingPattern, elapsedMillis: Long): BreathState {
        require(elapsedMillis >= 0) { "elapsedMillis must be >= 0" }
        val cycle = (elapsedMillis / pattern.cycleMillis).toInt()
        var offset = elapsedMillis % pattern.cycleMillis
        for (step in pattern.steps) {
            if (offset < step.durationMillis) {
                val progress = offset.toFloat() / step.durationMillis
                return BreathState(
                    phase = step.phase,
                    phaseProgress = progress,
                    phaseRemainingMillis = step.durationMillis - offset,
                    cycle = cycle,
                    ringScale = ringScale(step.phase, progress),
                )
            }
            offset -= step.durationMillis
        }
        error("Unreachable: offset beyond cycle length")
    }

    /** Number of phase changes between two instants; used to fire one haptic per change. */
    fun phaseIndexAt(pattern: BreathingPattern, elapsedMillis: Long): Long {
        val cycles = elapsedMillis / pattern.cycleMillis
        var offset = elapsedMillis % pattern.cycleMillis
        var index = 0
        for (step in pattern.steps) {
            if (offset < step.durationMillis) break
            offset -= step.durationMillis
            index++
        }
        return cycles * pattern.steps.size + index
    }

    private fun ringScale(phase: BreathPhase, progress: Float): Float = when (phase) {
        BreathPhase.INHALE -> easeInOut(progress)
        BreathPhase.HOLD_IN -> 1f
        BreathPhase.EXHALE -> 1f - easeInOut(progress)
        BreathPhase.HOLD_OUT -> 0f
    }

    private fun easeInOut(t: Float): Float = if (t < 0.5f) 2 * t * t else 1 - (-2 * t + 2).let { it * it } / 2
}
