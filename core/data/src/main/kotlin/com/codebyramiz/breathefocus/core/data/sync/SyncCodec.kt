package com.codebyramiz.breathefocus.core.data.sync

import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Wire format for sessions sent over the Wearable Data Layer. */
@Serializable
internal data class SessionDto(
    val id: String,
    val type: String,
    val pattern: String? = null,
    val startedAt: Long,
    val durationMillis: Long,
    val averageHeartRate: Int? = null,
)

object SyncCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(sessions: List<Session>): String = json.encodeToString(sessions.map { it.toDto() })

    /**
     * Splits [sessions] into JSON payloads of at most [maxBytes] UTF-8 bytes each, so every
     * payload fits in one Data Layer item. Each payload decodes on its own with [decode].
     */
    fun encodeChunks(sessions: List<Session>, maxBytes: Int): List<String> {
        val chunks = mutableListOf<String>()
        val current = mutableListOf<String>()
        var currentBytes = EMPTY_ARRAY_BYTES
        for (session in sessions) {
            val item = json.encodeToString(session.toDto())
            val itemBytes = item.toByteArray(Charsets.UTF_8).size
            // Every item after the first adds a "," separator.
            if (current.isNotEmpty() && currentBytes + 1 + itemBytes > maxBytes) {
                chunks += current.joinToString(",", "[", "]")
                current.clear()
                currentBytes = EMPTY_ARRAY_BYTES
            }
            currentBytes += itemBytes + if (current.isEmpty()) 0 else 1
            current += item
        }
        if (current.isNotEmpty() || chunks.isEmpty()) chunks += current.joinToString(",", "[", "]")
        return chunks
    }

    fun decode(payload: String): List<Session> = json.decodeFromString<List<SessionDto>>(payload).mapNotNull { dto ->
        val type = SessionType.entries.firstOrNull { it.name == dto.type } ?: return@mapNotNull null
        Session(
            id = dto.id,
            type = type,
            pattern = dto.pattern?.let { BreathingPattern.fromNameOrDefault(it) },
            startedAtEpochMillis = dto.startedAt,
            durationMillis = dto.durationMillis,
            averageHeartRate = dto.averageHeartRate,
        )
    }

    private fun Session.toDto() =
        SessionDto(id, type.name, pattern?.name, startedAtEpochMillis, durationMillis, averageHeartRate)

    // "[" and "]".
    private const val EMPTY_ARRAY_BYTES = 2
}
