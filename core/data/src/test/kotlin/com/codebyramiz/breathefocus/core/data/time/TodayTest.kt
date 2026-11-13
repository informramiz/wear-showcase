package com.codebyramiz.breathefocus.core.data.time

import com.codebyramiz.breathefocus.core.data.SchedulerClock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayTest {
    @Test
    fun `emits today and then each following day at local midnight`() = runTest {
        val clock = SchedulerClock(testScheduler, Instant.parse("2026-11-13T22:00:00Z"))

        val days = clock.today().take(3).toList()

        assertEquals(listOf(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15)), days)
        // The third day arrives exactly 26 hours after the start: midnight, then midnight again.
        assertEquals(26 * 60 * 60_000L, testScheduler.currentTime)
    }
}
