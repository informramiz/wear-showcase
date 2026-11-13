package com.codebyramiz.breathefocus.core.data

import com.codebyramiz.breathefocus.core.data.db.SessionDao
import com.codebyramiz.breathefocus.core.data.db.toEntity
import com.codebyramiz.breathefocus.core.data.db.toModel
import com.codebyramiz.breathefocus.core.model.Session
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single source of truth for finished sessions on this device. */
interface SessionRepository {
    fun observeAll(): Flow<List<Session>>
    fun observeSince(fromEpochMillis: Long): Flow<List<Session>>
    suspend fun getSince(fromEpochMillis: Long): List<Session>
    suspend fun save(session: Session)

    /** Idempotent: sessions are keyed by id, so replaying a sync batch is safe. */
    suspend fun upsertAll(sessions: List<Session>)
}

class RoomSessionRepository @Inject constructor(private val dao: SessionDao) : SessionRepository {
    override fun observeAll(): Flow<List<Session>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeSince(fromEpochMillis: Long): Flow<List<Session>> =
        dao.observeSince(fromEpochMillis).map { list -> list.map { it.toModel() } }

    override suspend fun getSince(fromEpochMillis: Long): List<Session> = dao.getSince(fromEpochMillis).map {
        it.toModel()
    }

    override suspend fun save(session: Session) = dao.upsert(listOf(session.toEntity()))

    override suspend fun upsertAll(sessions: List<Session>) = dao.upsert(sessions.map { it.toEntity() })
}
