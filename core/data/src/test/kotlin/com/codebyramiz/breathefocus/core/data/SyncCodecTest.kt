package com.codebyramiz.breathefocus.core.data

import com.codebyramiz.breathefocus.core.data.sync.DataLayerContract
import com.codebyramiz.breathefocus.core.data.sync.SyncCodec
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCodecTest {
    @Test
    fun `encode then decode round-trips sessions`() {
        val sessions = listOf(
            Session("1", SessionType.BREATHING, BreathingPattern.RELAX_478, 10, 120_000, 70),
            Session("2", SessionType.FOCUS, null, 20, 1_500_000, null),
        )
        assertEquals(sessions, SyncCodec.decode(SyncCodec.encode(sessions)))
    }

    @Test
    fun `unknown session types and fields are skipped`() {
        val payload = """[{"id":"x","type":"YOGA","startedAt":1,"durationMillis":2},""" +
            """{"id":"y","type":"FOCUS","startedAt":1,"durationMillis":2,"extra":true}]"""
        val decoded = SyncCodec.decode(payload)
        assertEquals(listOf("y"), decoded.map { it.id })
    }

    @Test
    fun `a month of sessions stays well under the 100 KB data item limit`() {
        val many = (1..300).map {
            Session("id-$it", SessionType.BREATHING, BreathingPattern.BOX, it * 1000L, 300_000, 65)
        }
        assertTrue(SyncCodec.encode(many).toByteArray().size < 100 * 1024)
    }

    @Test
    fun `large histories split into chunks that each fit a data item and decode back in full`() {
        val many = (1..1_500).map {
            Session(
                UUID.randomUUID().toString(),
                SessionType.BREATHING,
                BreathingPattern.RELAX_478,
                it * 1000L,
                300_000,
                65,
            )
        }
        assertTrue(SyncCodec.encode(many).toByteArray().size > 100 * 1024)

        val chunks = SyncCodec.encodeChunks(many, DataLayerContract.MAX_SESSION_CHUNK_BYTES)

        assertTrue(chunks.size > 1)
        chunks.forEach { assertTrue(it.toByteArray().size <= DataLayerContract.MAX_SESSION_CHUNK_BYTES) }
        assertEquals(many, chunks.flatMap { SyncCodec.decode(it) })
    }

    @Test
    fun `no sessions encode as one empty chunk, so stale chunks still get replaced`() {
        assertEquals(listOf("[]"), SyncCodec.encodeChunks(emptyList(), DataLayerContract.MAX_SESSION_CHUNK_BYTES))
    }

    @Test
    fun `chunk paths round-trip and other paths are rejected`() {
        assertEquals(3, DataLayerContract.sessionChunkIndex(DataLayerContract.sessionChunkPath(3)))
        assertEquals(null, DataLayerContract.sessionChunkIndex(DataLayerContract.PATH_SESSIONS))
        assertEquals(null, DataLayerContract.sessionChunkIndex(DataLayerContract.PATH_SETTINGS))
        assertEquals(null, DataLayerContract.sessionChunkIndex(null))
    }
}
