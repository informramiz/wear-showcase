package com.codebyramiz.breathefocus.core.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.TestCoroutineScheduler

/** A wall clock that moves with the test scheduler's virtual time, starting at [start]. */
class SchedulerClock(
    private val scheduler: TestCoroutineScheduler,
    private val start: Instant,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = SchedulerClock(scheduler, start, zone)

    override fun instant(): Instant = start.plusMillis(scheduler.currentTime)
}
