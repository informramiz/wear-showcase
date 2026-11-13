package com.codebyramiz.breathefocus.core.data.demo

import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

/**
 * Debug builds only: a believable week of history for screenshots and demos on emulators,
 * where no real watch is paired. Never compiled into release.
 */
object DemoSessions {
    private val plan = listOf(
        // daysAgo, hour, minutes, type, pattern, heart rate
        Row(6, 8, 6, SessionType.BREATHING, BreathingPattern.BOX, 68),
        Row(5, 13, 25, SessionType.FOCUS, null, null),
        Row(5, 21, 4, SessionType.BREATHING, BreathingPattern.RELAX_478, 61),
        Row(4, 9, 12, SessionType.BREATHING, BreathingPattern.COHERENT, 64),
        Row(3, 7, 5, SessionType.BREATHING, BreathingPattern.BOX, 70),
        Row(2, 10, 25, SessionType.FOCUS, null, null),
        Row(2, 22, 8, SessionType.BREATHING, BreathingPattern.RELAX_478, 59),
        Row(1, 8, 14, SessionType.BREATHING, BreathingPattern.COHERENT, 63),
        Row(0, 7, 4, SessionType.BREATHING, BreathingPattern.BOX, 66),
        Row(0, 9, 3, SessionType.BREATHING, BreathingPattern.COHERENT, 62),
    )

    fun generate(clock: Clock): List<Session> {
        val today = LocalDate.now(clock)
        return plan.map { row ->
            val start = today.minusDays(row.daysAgo.toLong()).atTime(LocalTime.of(row.hour, 0)).atZone(clock.zone)
            Session(
                id = "demo-${row.daysAgo}-${row.hour}",
                type = row.type,
                pattern = row.pattern,
                startedAtEpochMillis = start.toInstant().toEpochMilli(),
                durationMillis = row.minutes * 60_000L,
                averageHeartRate = row.heartRate,
            )
        }
    }

    private data class Row(
        val daysAgo: Int,
        val hour: Int,
        val minutes: Int,
        val type: SessionType,
        val pattern: BreathingPattern?,
        val heartRate: Int?,
    )
}
