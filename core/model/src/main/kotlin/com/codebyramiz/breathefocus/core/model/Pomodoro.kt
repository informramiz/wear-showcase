package com.codebyramiz.breathefocus.core.model

enum class FocusPhase { FOCUS, BREAK }

data class PomodoroConfig(
    val focusMillis: Long = 25 * 60_000L,
    val breakMillis: Long = 5 * 60_000L,
)

data class PomodoroState(
    val phase: FocusPhase,
    val remainingMillis: Long,
    val progress: Float,
    val completedFocusBlocks: Int,
)

/** Pure Pomodoro timing: alternates focus and break blocks from a start instant. */
object Pomodoro {

    fun stateAt(config: PomodoroConfig, elapsedMillis: Long): PomodoroState {
        require(elapsedMillis >= 0) { "elapsedMillis must be >= 0" }
        val round = config.focusMillis + config.breakMillis
        val completedRounds = (elapsedMillis / round).toInt()
        val offset = elapsedMillis % round
        return if (offset < config.focusMillis) {
            PomodoroState(
                phase = FocusPhase.FOCUS,
                remainingMillis = config.focusMillis - offset,
                progress = offset.toFloat() / config.focusMillis,
                completedFocusBlocks = completedRounds,
            )
        } else {
            val breakOffset = offset - config.focusMillis
            PomodoroState(
                phase = FocusPhase.BREAK,
                remainingMillis = config.breakMillis - breakOffset,
                progress = breakOffset.toFloat() / config.breakMillis,
                completedFocusBlocks = completedRounds + 1,
            )
        }
    }

    /** Minutes of actual focus (breaks excluded) in [elapsedMillis]. */
    fun focusedMillis(config: PomodoroConfig, elapsedMillis: Long): Long {
        val round = config.focusMillis + config.breakMillis
        val full = elapsedMillis / round
        val offset = elapsedMillis % round
        return full * config.focusMillis + minOf(offset, config.focusMillis)
    }
}
