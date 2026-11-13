package com.codebyramiz.breathefocus.mobile.sync

import com.codebyramiz.breathefocus.core.data.sync.SyncCodec
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.mobile.FakeSessionRepository
import com.codebyramiz.breathefocus.mobile.session
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionImporterTest {
    private val repo = FakeSessionRepository()
    private val importer = SessionImporter(repo)

    @Test
    fun `imports the watch payload and stays idempotent on resync`() = runTest {
        val sent = listOf(
            session("a", 1_000L, minutes = 4, hr = 70),
            session("b", 2_000L, minutes = 25, type = SessionType.FOCUS),
        )
        val payload = SyncCodec.encode(sent)

        assertEquals(2, importer.import(payload))
        importer.import(payload)

        assertEquals(sent.toSet(), repo.sessions.value.toSet())
    }

    @Test
    fun `malformed payload fails with IllegalArgumentException and writes nothing`() = runTest {
        val error = runCatching { importer.import("not json") }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(emptyList<Any>(), repo.sessions.value)
    }
}
