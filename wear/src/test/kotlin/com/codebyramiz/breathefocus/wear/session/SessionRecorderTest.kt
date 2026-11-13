package com.codebyramiz.breathefocus.wear.session

import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakeSessionRepository
import com.codebyramiz.breathefocus.wear.FakeSync
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionRecorderTest {
    private val now = Instant.parse("2026-11-13T09:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun session(id: String, daysAgo: Long) = Session(
        id = id,
        type = SessionType.BREATHING,
        pattern = BreathingPattern.BOX,
        startedAtEpochMillis = now.minus(Duration.ofDays(daysAgo)).toEpochMilli(),
        durationMillis = 120_000,
        averageHeartRate = null,
    )

    @Test
    fun `publishes only the last 30 days to the phone`() = runTest {
        val repository = FakeSessionRepository().apply { save(session("old", 45)) }
        val sync = FakeSync()
        SessionRecorder(repository, sync, {}, clock).record(session("new", 0))

        assertEquals(listOf("new"), sync.published.single().map { it.id })
        assertEquals(2, repository.sessions.value.size)
    }

    @Test
    fun `a disconnected phone does not lose the session`() = runTest {
        val repository = FakeSessionRepository()
        var refreshed = false
        SessionRecorder(repository, FakeSync(fail = true), { refreshed = true }, clock).record(session("a", 0))

        assertEquals(1, repository.sessions.value.size)
        assertEquals(true, refreshed)
    }
}
