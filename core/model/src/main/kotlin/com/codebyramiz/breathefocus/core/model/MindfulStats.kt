package com.codebyramiz.breathefocus.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayTotal(val date: LocalDate, val minutes: Int)

/** Aggregations over sessions. Time zone is injected so tests stay deterministic. */
class MindfulStats(private val zone: ZoneId) {

    fun minutesOn(date: LocalDate, sessions: List<Session>): Int = sessions
        .filter { it.localDate() == date }
        .sumOf { it.durationMillis }
        .let { (it / 60_000L).toInt() }

    /** Seven [DayTotal]s ending on [today], oldest first; days without sessions are 0. */
    fun lastSevenDays(today: LocalDate, sessions: List<Session>): List<DayTotal> {
        val byDay = sessions.groupBy { it.localDate() }
        return (6 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            val millis = byDay[day].orEmpty().sumOf { it.durationMillis }
            DayTotal(day, (millis / 60_000L).toInt())
        }
    }

    /** Consecutive days up to [today] with at least one minute. */
    fun streak(today: LocalDate, sessions: List<Session>): Int {
        val days = sessions.filter { it.durationMillis >= 60_000L }.map { it.localDate() }.toSet()
        var count = 0
        var day = today
        while (day in days) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    private fun Session.localDate(): LocalDate = Instant.ofEpochMilli(startedAtEpochMillis).atZone(zone).toLocalDate()

    companion object {
        const val DAILY_GOAL_MINUTES = 10
    }
}
