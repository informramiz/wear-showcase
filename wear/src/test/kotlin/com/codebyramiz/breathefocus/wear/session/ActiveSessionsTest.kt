package com.codebyramiz.breathefocus.wear.session

import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakePhaseAlarms
import com.codebyramiz.breathefocus.wear.FakeSessionStore
import com.codebyramiz.breathefocus.wear.FakeSync
import com.codebyramiz.breathefocus.wear.FakeTicker
import com.codebyramiz.breathefocus.wear.SchedulerClock
import com.codebyramiz.breathefocus.wear.testActiveSessions
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log on the publish path.
@RunWith(RobolectricTestRunner::class)
class ActiveSessionsTest {
    private val store = FakeSessionStore()
    private val alarms = FakePhaseAlarms()
    private val sync = FakeSync()

    private fun activeSessions(scope: TestScope) = testActiveSessions(
        store,
        sync,
        alarms,
        FakeTicker(scope.testScheduler),
        SchedulerClock(scope.testScheduler, Instant.parse("2026-11-13T09:00:00Z")),
        CoroutineScope(UnconfinedTestDispatcher(scope.testScheduler)),
    )

    @Test
    fun `begins a session and persists it`() = runTest {
        val sessions = activeSessions(this)

        val started = sessions.resumeOrBegin(SessionType.BREATHING, BreathingPattern.BOX)

        assertEquals(started, store.active)
    }

    @Test
    fun `resumes the persisted session of the same type`() = runTest {
        val sessions = activeSessions(this)
        val first = sessions.resumeOrBegin(SessionType.BREATHING, BreathingPattern.BOX)
        advanceTimeBy(5_000)

        val again = sessions.resumeOrBegin(SessionType.BREATHING, BreathingPattern.COHERENT)

        assertEquals(first, again)
    }

    @Test
    fun `a persisted session of another type is finished before a new one begins`() = runTest {
        val sessions = activeSessions(this)
        val focus = sessions.resumeOrBegin(SessionType.FOCUS, null)
        advanceTimeBy(10 * 60_000L)

        val breathing = sessions.resumeOrBegin(SessionType.BREATHING, BreathingPattern.BOX)

        assertNotEquals(focus.id, breathing.id)
        assertEquals(breathing, store.active)
        val saved = store.sessions.value.single()
        assertEquals(focus.id, saved.id)
        assertEquals(10 * 60_000L, saved.durationMillis)
    }

    @Test
    fun `recoverOrphan records what a dead process left behind and clears it`() = runTest {
        val sessions = activeSessions(this)
        val orphan = sessions.resumeOrBegin(SessionType.BREATHING, BreathingPattern.BOX)
        advanceTimeBy(3 * 60_000L)

        sessions.recoverOrphan().join()

        assertNull(store.active)
        val saved = store.sessions.value.single()
        assertEquals(orphan.id, saved.id)
        assertEquals(3 * 60_000L, saved.durationMillis)
        assertEquals(listOf(saved.id), sync.published.single().map { it.id })
    }

    @Test
    fun `recovering twice records the session once`() = runTest {
        val sessions = activeSessions(this)
        sessions.resumeOrBegin(SessionType.BREATHING, BreathingPattern.BOX)
        advanceTimeBy(3 * 60_000L)

        sessions.recoverOrphan().join()
        sessions.recoverOrphan().join()

        assertEquals(1, store.sessions.value.size)
    }

    @Test
    fun `a recovered focus session counts only its focused time`() = runTest {
        val sessions = activeSessions(this)
        sessions.resumeOrBegin(SessionType.FOCUS, null)
        advanceTimeBy(28 * 60_000L) // 25 focus + 3 break

        sessions.recoverOrphan().join()

        assertEquals(25 * 60_000L, store.sessions.value.single().durationMillis)
    }

    @Test
    fun `finish cancels the alarm and saves the session`() = runTest {
        val sessions = activeSessions(this)
        val started = sessions.resumeOrBegin(SessionType.BREATHING, BreathingPattern.BOX)
        alarms.schedule(1)

        sessions.finish(
            Session(
                id = started.id,
                type = SessionType.BREATHING,
                pattern = BreathingPattern.BOX,
                startedAtEpochMillis = started.startedAtEpochMillis,
                durationMillis = 90_000,
                averageHeartRate = null,
            ),
        ).join()

        assertNull(alarms.triggerAt)
        assertNull(store.active)
        assertEquals(started.id, store.sessions.value.single().id)
    }

    @Test
    fun `an unfinished record is left alone when there is nothing to recover`() = runTest {
        val sessions = activeSessions(this)

        sessions.recoverOrphan().join()

        assertEquals(emptyList<Any>(), store.sessions.value)
    }

    @Test
    fun `recovered time follows the monotonic clock when both clocks agree`() {
        assertEquals(
            120_000L,
            ActiveSessions.recoveredElapsedMillis(monotonicMillis = 120_000, wallClockMillis = 125_000),
        )
    }

    @Test
    fun `recovered time follows the wall clock after a reboot`() {
        // The monotonic clock restarted, so it reads far less than the wall clock.
        assertEquals(
            600_000L,
            ActiveSessions.recoveredElapsedMillis(monotonicMillis = 5_000, wallClockMillis = 600_000),
        )
        assertEquals(
            600_000L,
            ActiveSessions.recoveredElapsedMillis(monotonicMillis = -3_000, wallClockMillis = 600_000),
        )
    }

    @Test
    fun `a wall-clock adjustment without a reboot keeps the monotonic time`() {
        // The user moved the clock back an hour; the boot count is unchanged, so only the monotonic clock counts.
        assertEquals(
            120_000L,
            ActiveSessions.recoveredElapsedMillis(120_000, wallClockMillis = -3_480_000, rebooted = false),
        )
        assertEquals(
            120_000L,
            ActiveSessions.recoveredElapsedMillis(120_000, wallClockMillis = 7_200_000, rebooted = false),
        )
    }

    @Test
    fun `recovered time is capped`() {
        assertEquals(
            ActiveSessions.MAX_RECOVERED_MILLIS,
            ActiveSessions.recoveredElapsedMillis(monotonicMillis = 5_000, wallClockMillis = 20 * 60 * 60_000L),
        )
        assertEquals(0L, ActiveSessions.recoveredElapsedMillis(monotonicMillis = 5_000, wallClockMillis = -10_000_000))
    }
}
