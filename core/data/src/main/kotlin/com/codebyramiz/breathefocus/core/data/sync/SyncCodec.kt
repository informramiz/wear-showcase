package com.codebyramiz.breathefocus.core.data.sync

import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Wire format for sessions sent over the Wearable Data Layer (kept under the 100 KB item limit). */
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

    fun encode(sessions: List<Session>): String = json.encodeToString(
        sessions.map {
            SessionDto(
                it.id,
                it.type.name,
                it.pattern?.name,
                it.startedAtEpochMillis,
                it.durationMillis,
                it.averageHeartRate,
            )
        },
    )

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
}
