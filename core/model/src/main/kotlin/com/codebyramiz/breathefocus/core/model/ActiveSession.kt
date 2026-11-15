package com.codebyramiz.breathefocus.core.model

/**
 * A session that has started but not finished. It is persisted so a process death does not lose it.
 *
 * @property id the id the finished [Session] will carry, so finishing is idempotent.
 * @property startedAtElapsedRealtime start on the `SystemClock.elapsedRealtime` time base.
 * @property startedAtEpochMillis start on the wall clock; used if the elapsed time base was reset by a reboot.
 */
data class ActiveSession(
    val id: String,
    val type: SessionType,
    val pattern: BreathingPattern?,
    val startedAtElapsedRealtime: Long,
    val startedAtEpochMillis: Long,
)
