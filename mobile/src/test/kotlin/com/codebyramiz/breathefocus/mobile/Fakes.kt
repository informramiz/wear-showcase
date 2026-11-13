package com.codebyramiz.breathefocus.mobile

import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSessionRepository : SessionRepository {
    val sessions = MutableStateFlow<List<Session>>(emptyList())

    override fun observeAll(): Flow<List<Session>> = sessions

    override fun observeSince(fromEpochMillis: Long): Flow<List<Session>> =
        sessions.map { list -> list.filter { it.startedAtEpochMillis >= fromEpochMillis } }

    override suspend fun getSince(fromEpochMillis: Long): List<Session> =
        sessions.value.filter { it.startedAtEpochMillis >= fromEpochMillis }

    override suspend fun save(session: Session) {
        sessions.value = sessions.value.filterNot { it.id == session.id } + session
    }

    override suspend fun upsertAll(sessions: List<Session>) = sessions.forEach { save(it) }
}

class FakeSettings(initial: BreathingPattern = BreathingPattern.DEFAULT) : SettingsRepository {
    val pattern = MutableStateFlow(initial)
    override val defaultPattern: Flow<BreathingPattern> = pattern

    override suspend fun setDefaultPattern(pattern: BreathingPattern) {
        this.pattern.value = pattern
    }
}

class FakeSync(private val fail: Boolean = false) : DataLayerSync {
    val patterns = mutableListOf<BreathingPattern>()

    override suspend fun publishSessions(sessions: List<Session>) = Unit

    override suspend fun publishDefaultPattern(pattern: BreathingPattern) {
        if (fail) error("watch not connected")
        patterns += pattern
    }
}

fun session(id: String, startedAt: Long, minutes: Int, type: SessionType = SessionType.BREATHING, hr: Int? = null) =
    Session(
        id = id,
        type = type,
        pattern = if (type == SessionType.BREATHING) BreathingPattern.BOX else null,
        startedAtEpochMillis = startedAt,
        durationMillis = minutes * 60_000L,
        averageHeartRate = hr,
    )
