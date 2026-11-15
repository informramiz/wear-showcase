package com.codebyramiz.breathefocus.wear.session

import com.codebyramiz.breathefocus.core.data.sync.DataLayerCoordinator
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.FakeSessionStore
import com.codebyramiz.breathefocus.wear.FakeSync
import com.codebyramiz.breathefocus.wear.testPublisher
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
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
    private val store = FakeSessionStore()

    private fun session(id: String) =
        Session(id, SessionType.BREATHING, BreathingPattern.BOX, now.toEpochMilli(), 120_000, null)

    private suspend fun finishOne(id: String = "a") = store.finish(session(id))

    @Test
    fun `a failed push stays queued and a fresh publisher retries it`() = runTest {
        finishOne()
        val sync = FakeSync(fail = true)

        assertFalse(testPublisher(store, sync, clock).publishRecent())
        assertEquals(listOf("a"), store.pendingIds())

        // A new process: the queue is in the store, not in the publisher.
        sync.fail = false
        testPublisher(store, sync, clock).retryIfPending()

        assertEquals(listOf("a"), sync.published.single().map { it.id })
        assertEquals(emptyList<String>(), store.pendingIds())
    }

    @Test
    fun `an interruption between saving and publishing is recovered on the next start`() = runTest {
        // The process died right after the transaction that saved the session and queued it.
        finishOne()

        val sync = FakeSync()
        testPublisher(store, sync, clock).retryIfPending()

        assertEquals(listOf("a"), sync.published.single().map { it.id })
        assertTrue(store.pendingIds().isEmpty())
    }

    @Test
    fun `retry does nothing when nothing is queued`() = runTest {
        val sync = FakeSync()

        testPublisher(store, sync, clock).retryIfPending()

        assertTrue(sync.published.isEmpty())
    }

    @Test
    fun `a session finished while a push is in flight stays queued for the next one`() = runTest {
        finishOne("a")
        val gate = CompletableDeferred<Unit>()
        val published = mutableListOf<List<String>>()
        val sync = object : DataLayerSync {
            override suspend fun publishSessions(sessions: List<Session>) {
                if (published.isEmpty()) {
                    // "b" finishes while the first push is on its way, after "a" was snapshotted.
                    finishOne("b")
                    gate.await()
                }
                published += sessions.map { it.id }
            }

            override suspend fun publishDefaultPattern(pattern: BreathingPattern) = Unit
        }
        val publisher = testPublisher(store, sync, clock)

        val first = launch { publisher.publishRecent() }
        runCurrent()
        gate.complete(Unit)
        first.join()

        assertEquals(listOf("b"), store.pendingIds())
        publisher.retryIfPending()
        assertEquals(emptyList<String>(), store.pendingIds())
    }

    @Test
    fun `publications never interleave`() = runTest {
        finishOne()
        val gate = CompletableDeferred<Unit>()
        var inFlight = 0
        var maxInFlight = 0
        val sync = object : DataLayerSync {
            override suspend fun publishSessions(sessions: List<Session>) {
                inFlight++
                maxInFlight = maxOf(maxInFlight, inFlight)
                gate.await()
                inFlight--
            }

            override suspend fun publishDefaultPattern(pattern: BreathingPattern) = Unit
        }
        val coordinator = DataLayerCoordinator()
        val one = launch { testPublisher(store, sync, clock, coordinator).publishRecent() }
        val two = launch { testPublisher(store, sync, clock, coordinator).publishRecent() }
        runCurrent()

        assertEquals(1, inFlight)
        gate.complete(Unit)
        one.join()
        two.join()

        assertEquals(1, maxInFlight)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is not treated as a failed push`() = runTest {
        finishOne()
        val cancelling = object : DataLayerSync {
            override suspend fun publishSessions(sessions: List<Session>) = throw CancellationException("left")

            override suspend fun publishDefaultPattern(pattern: BreathingPattern) = Unit
        }
        try {
            testPublisher(store, cancelling, clock).publishRecent()
        } finally {
            assertEquals(listOf("a"), store.pendingIds())
        }
    }
}
