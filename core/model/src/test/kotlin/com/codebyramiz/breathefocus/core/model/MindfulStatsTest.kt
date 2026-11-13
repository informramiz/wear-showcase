package com.codebyramiz.breathefocus.core.model

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class MindfulStatsTest {
    private val stats = MindfulStats(ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 11, 13)

    private fun session(day: LocalDate, minutes: Int, id: String = "$day-$minutes") = Session(
        id = id,
        type = SessionType.BREATHING,
        pattern = BreathingPattern.BOX,
        startedAtEpochMillis = day.atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
        durationMillis = minutes * 60_000L,
        averageHeartRate = null,
    )

    @Test
    fun `minutes today sums only today`() {
        val sessions = listOf(session(today, 3), session(today, 4, "b"), session(today.minusDays(1), 9))
        assertEquals(7, stats.minutesOn(today, sessions))
    }

    @Test
    fun `last seven days fills gaps with zero, oldest first`() {
        val week = stats.lastSevenDays(today, listOf(session(today, 5), session(today.minusDays(6), 2)))
        assertEquals(7, week.size)
        assertEquals(today.minusDays(6), week.first().date)
        assertEquals(2, week.first().minutes)
        assertEquals(0, week[3].minutes)
        assertEquals(5, week.last().minutes)
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val sessions = listOf(session(today, 2), session(today.minusDays(1), 1), session(today.minusDays(3), 4))
        assertEquals(2, stats.streak(today, sessions))
        assertEquals(0, stats.streak(today.plusDays(1), sessions))
    }
}
