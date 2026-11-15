package com.codebyramiz.breathefocus.mobile

import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.sync.DataLayerReader
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

    override suspend fun finish(session: Session) = upsertAll(listOf(session))

    override suspend fun upsertAll(sessions: List<Session>) {
        val ids = sessions.map { it.id }.toSet()
        this.sessions.value = this.sessions.value.filterNot { it.id in ids } + sessions
    }
}

class FakeSettings(initial: BreathingPattern = BreathingPattern.DEFAULT) : SettingsRepository {
    val pattern = MutableStateFlow(initial)
    var pending: BreathingPattern? = null
    override val defaultPattern: Flow<BreathingPattern> = pattern

    override suspend fun setDefaultPattern(pattern: BreathingPattern) {
        this.pattern.value = pattern
    }

    override suspend fun selectDefaultPattern(pattern: BreathingPattern) {
        this.pattern.value = pattern
        pending = pattern
    }

    override suspend fun pendingPublication(): BreathingPattern? = pending

    override suspend fun markPublished(pattern: BreathingPattern) {
        if (pending == pattern) pending = null
    }
}

class FakeSync(var fail: Boolean = false) : DataLayerSync {
    val patterns = mutableListOf<BreathingPattern>()

    override suspend fun publishSessions(sessions: List<Session>) = Unit

    override suspend fun publishDefaultPattern(pattern: BreathingPattern) {
        if (fail) error("watch not connected")
        patterns += pattern
    }
}

class FakeDataLayerReader(var payloads: List<String> = emptyList(), var pattern: BreathingPattern? = null) :
    DataLayerReader {
    override suspend fun sessionPayloads(): List<String> = payloads

    override suspend fun defaultPattern(): BreathingPattern? = pattern
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
