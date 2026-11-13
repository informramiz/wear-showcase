package com.codebyramiz.breathefocus.core.model

/** One step of a breathing cycle. */
enum class BreathPhase(val label: String) {
    INHALE("Breathe in"),
    HOLD_IN("Hold"),
    EXHALE("Breathe out"),
    HOLD_OUT("Hold"),
}

/** A timed step inside a [BreathingPattern]. */
data class PhaseStep(val phase: BreathPhase, val durationMillis: Long)

/**
 * The guided breathing patterns the app offers. Each is a repeating cycle of [steps];
 * zero-length holds are simply left out.
 */
enum class BreathingPattern(val displayName: String, val summary: String, val steps: List<PhaseStep>) {
    BOX(
        displayName = "Box",
        summary = "4 · 4 · 4 · 4",
        steps = listOf(
            PhaseStep(BreathPhase.INHALE, 4_000),
            PhaseStep(BreathPhase.HOLD_IN, 4_000),
            PhaseStep(BreathPhase.EXHALE, 4_000),
            PhaseStep(BreathPhase.HOLD_OUT, 4_000),
        ),
    ),
    RELAX_478(
        displayName = "4-7-8",
        summary = "4 · 7 · 8",
        steps = listOf(
            PhaseStep(BreathPhase.INHALE, 4_000),
            PhaseStep(BreathPhase.HOLD_IN, 7_000),
            PhaseStep(BreathPhase.EXHALE, 8_000),
        ),
    ),
    COHERENT(
        displayName = "Coherent",
        summary = "5.5 · 5.5",
        steps = listOf(
            PhaseStep(BreathPhase.INHALE, 5_500),
            PhaseStep(BreathPhase.EXHALE, 5_500),
        ),
    ),
    ;

    val cycleMillis: Long = steps.sumOf { it.durationMillis }

    companion object {
        val DEFAULT = BOX

        fun fromNameOrDefault(name: String?): BreathingPattern = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
