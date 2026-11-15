package com.codebyramiz.breathefocus.core.data.time

import com.codebyramiz.breathefocus.core.data.SchedulerClock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayTest {
    @Test
    fun `emits today and then each following day at local midnight`() = runTest {
        val clock = SchedulerClock(testScheduler, Instant.parse("2026-11-13T22:00:00Z"))

        val days = clock.today(emptyFlow()).take(3).toList()

        assertEquals(listOf(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15)), days)
        // The third day arrives exactly 26 hours after the start: midnight, then midnight again.
        assertEquals(26 * 60 * 60_000L, testScheduler.currentTime)
    }

    @Test
    fun `a clock adjustment moves the date at once instead of at the old midnight`() = runTest {
        val base = SchedulerClock(testScheduler, Instant.parse("2026-11-13T10:00:00Z"))
        var jump = Duration.ZERO
        val clock = object : Clock() {
            override fun getZone(): ZoneId = base.zone

            override fun withZone(zone: ZoneId): Clock = base.withZone(zone)

            override fun instant(): Instant = base.instant().plus(jump)
        }
        val changes = MutableSharedFlow<Unit>()
        val days = mutableListOf<LocalDate>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { clock.today(changes).toList(days) }

        jump = Duration.ofDays(2)
        changes.emit(Unit)

        assertEquals(listOf(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 15)), days)
        assertEquals(0L, testScheduler.currentTime)
        job.cancel()
    }
}
