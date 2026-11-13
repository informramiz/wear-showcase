package com.codebyramiz.breathefocus.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.codebyramiz.breathefocus.core.data.db.BreatheDatabase
import com.codebyramiz.breathefocus.core.model.ActiveSession
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomSessionRepositoryTest {
    private lateinit var db: BreatheDatabase
    private lateinit var repo: RoomSessionRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BreatheDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomSessionRepository(db.sessionDao())
    }

    @After
    fun tearDown() = db.close()

    private fun session(id: String, startedAt: Long, minutes: Int = 3) = Session(
        id = id,
        type = SessionType.BREATHING,
        pattern = BreathingPattern.COHERENT,
        startedAtEpochMillis = startedAt,
        durationMillis = minutes * 60_000L,
        averageHeartRate = 64,
    )

    private fun active(id: String) = ActiveSession(
        id = id,
        type = SessionType.BREATHING,
        pattern = BreathingPattern.COHERENT,
        startedAtElapsedRealtime = 5_000,
        startedAtEpochMillis = 1_000,
    )

    @Test
    fun `finished sessions are observed newest first`() = runTest {
        repo.finish(session("a", 1_000))
        repo.finish(session("b", 2_000))
        repo.observeAll().test {
            assertEquals(listOf("b", "a"), awaitItem().map { it.id })
        }
    }

    @Test
    fun `finishing saves the session, queues it and clears the active record together`() = runTest {
        repo.begin(active("a"))
        repo.finish(session("a", 1_000))
        assertEquals(listOf("a"), repo.getSince(0).map { it.id })
        assertEquals(listOf("a"), repo.pendingIds())
        assertNull(repo.current())
    }

    @Test
    fun `finishing twice keeps one session and one outbox entry`() = runTest {
        repo.finish(session("a", 1_000))
        repo.finish(session("a", 1_000))
        assertEquals(1, repo.getSince(0).size)
        assertEquals(listOf("a"), repo.pendingIds())
    }

    @Test
    fun `an active session survives until it is finished`() = runTest {
        repo.begin(active("a"))
        assertEquals(active("a"), repo.current())
        repo.begin(active("b"))
        assertEquals("b", repo.current()?.id)
        repo.clear("b")
        assertNull(repo.current())
    }

    @Test
    fun `acknowledging removes only the published entries`() = runTest {
        repo.finish(session("a", 1_000))
        repo.finish(session("b", 2_000))
        repo.acknowledge(listOf("a"))
        assertEquals(listOf("b"), repo.pendingIds())
    }

    @Test
    fun `a session finished after the snapshot stays queued when the snapshot is acknowledged`() = runTest {
        repo.finish(session("a", 1_000))
        val snapshot = repo.pendingIds()
        repo.finish(session("b", 2_000))
        repo.acknowledge(snapshot)
        assertEquals(listOf("b"), repo.pendingIds())
    }

    @Test
    fun `upsertAll is idempotent by id`() = runTest {
        val batch = listOf(session("a", 1_000), session("b", 2_000))
        repo.upsertAll(batch)
        repo.upsertAll(batch)
        repo.upsertAll(listOf(session("a", 1_000, minutes = 9)))
        val all = repo.getSince(0)
        assertEquals(2, all.size)
        assertEquals(9, all.first { it.id == "a" }.durationMinutes)
    }

    @Test
    fun `getSince filters by start time and round-trips fields`() = runTest {
        repo.upsertAll(listOf(session("old", 500), session("new", 5_000)))
        val recent = repo.getSince(1_000)
        assertEquals(listOf("new"), recent.map { it.id })
        assertEquals(BreathingPattern.COHERENT, recent.single().pattern)
        assertEquals(64, recent.single().averageHeartRate)
    }
}
