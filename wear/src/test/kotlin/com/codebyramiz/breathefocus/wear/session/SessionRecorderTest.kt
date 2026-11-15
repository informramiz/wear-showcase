package com.codebyramiz.breathefocus.wear.session

import com.codebyramiz.breathefocus.core.model.ActiveSession
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakeRefresher
import com.codebyramiz.breathefocus.wear.FakeSessionStore
import com.codebyramiz.breathefocus.wear.FakeSync
import com.codebyramiz.breathefocus.wear.testRecorder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log on the failure path.
@RunWith(RobolectricTestRunner::class)
class SessionRecorderTest {
    private val now = Instant.parse("2026-11-13T09:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val store = FakeSessionStore()

    private fun session(id: String, daysAgo: Long, durationMillis: Long = 120_000) = Session(
        id = id,
        type = SessionType.BREATHING,
        pattern = BreathingPattern.BOX,
        startedAtEpochMillis = now.minus(Duration.ofDays(daysAgo)).toEpochMilli(),
        durationMillis = durationMillis,
        averageHeartRate = null,
    )

    @Test
    fun `publishes only the last 30 days to the phone`() = runTest {
        store.upsertAll(listOf(session("old", 45)))
        val sync = FakeSync()

        val recorder = testRecorder(store, sync, clock)
        recorder.record(session("new", 0))
        recorder.publish()

        assertEquals(listOf("new"), sync.published.single().map { it.id })
        assertEquals(2, store.sessions.value.size)
    }

    @Test
    fun `a disconnected phone does not lose the session and keeps it queued`() = runTest {
        val refresher = FakeRefresher()

        testRecorder(store, FakeSync(fail = true), clock, refresher).record(session("a", 0))

        assertEquals(1, store.sessions.value.size)
        assertEquals(listOf("a"), store.pendingIds())
        assertEquals(1, refresher.refreshes)
    }

    @Test
    fun `finishing clears the active record and is idempotent`() = runTest {
        store.begin(ActiveSession("a", SessionType.BREATHING, BreathingPattern.BOX, 0, now.toEpochMilli()))
        val recorder = testRecorder(store, FakeSync(), clock)

        recorder.record(session("a", 0))
        recorder.record(session("a", 0))

        assertNull(store.current())
        assertEquals(1, store.sessions.value.size)
    }

    @Test
    fun `a session under a minute is dropped and its active record cleared`() = runTest {
        store.begin(ActiveSession("a", SessionType.BREATHING, BreathingPattern.BOX, 0, now.toEpochMilli()))
        val sync = FakeSync()

        testRecorder(store, sync, clock).record(session("a", 0, durationMillis = 30_000))

        assertNull(store.current())
        assertEquals(emptyList<Session>(), store.sessions.value)
        assertEquals(emptyList<List<Session>>(), sync.published)
    }
}
