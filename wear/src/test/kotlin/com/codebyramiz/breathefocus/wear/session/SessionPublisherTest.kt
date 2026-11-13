package com.codebyramiz.breathefocus.wear.session

import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakePendingSync
import com.codebyramiz.breathefocus.wear.FakeSessionRepository
import com.codebyramiz.breathefocus.wear.FakeSync
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log on the failure path.
@RunWith(RobolectricTestRunner::class)
class SessionPublisherTest {
    private val now = Instant.parse("2026-11-13T09:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val repository = FakeSessionRepository()
    private val pending = FakePendingSync()

    private suspend fun saveOne() = repository.save(
        Session("a", SessionType.BREATHING, BreathingPattern.BOX, now.toEpochMilli(), 120_000, null),
    )

    @Test
    fun `a failed push is remembered and retried until it succeeds`() = runTest {
        saveOne()
        val sync = FakeSync(fail = true)
        val publisher = SessionPublisher(repository, sync, pending, clock)

        assertFalse(publisher.publishRecent())
        assertTrue(pending.pending)

        sync.fail = false
        publisher.retryIfPending()

        assertEquals(listOf("a"), sync.published.single().map { it.id })
        assertFalse(pending.pending)
    }

    @Test
    fun `retry does nothing when nothing is pending`() = runTest {
        val sync = FakeSync()
        SessionPublisher(repository, sync, pending, clock).retryIfPending()

        assertTrue(sync.published.isEmpty())
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is not treated as a failed push`() = runTest {
        val cancelling = object : DataLayerSync {
            override suspend fun publishSessions(sessions: List<Session>) = throw CancellationException("left")

            override suspend fun publishDefaultPattern(pattern: BreathingPattern) = Unit
        }
        try {
            SessionPublisher(repository, cancelling, pending, clock).publishRecent()
        } finally {
            assertFalse(pending.pending)
        }
    }
}
