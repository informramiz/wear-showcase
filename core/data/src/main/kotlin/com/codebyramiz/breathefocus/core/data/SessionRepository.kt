package com.codebyramiz.breathefocus.core.data

import com.codebyramiz.breathefocus.core.data.db.SessionDao
import com.codebyramiz.breathefocus.core.data.db.toEntity
import com.codebyramiz.breathefocus.core.data.db.toModel
import com.codebyramiz.breathefocus.core.model.ActiveSession
import com.codebyramiz.breathefocus.core.model.Session
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single source of truth for finished sessions on this device. */
interface SessionRepository {
    fun observeAll(): Flow<List<Session>>
    fun observeSince(fromEpochMillis: Long): Flow<List<Session>>
    suspend fun getSince(fromEpochMillis: Long): List<Session>

    /**
     * Saves [session], queues it in the [SessionOutbox] and clears its [ActiveSessionStore] record in
     * one transaction, so a crash can never leave a saved session that is not queued for the phone.
     * Idempotent: the session id is the key.
     */
    suspend fun finish(session: Session)

    /** Idempotent: sessions are keyed by id, so replaying a sync batch is safe. */
    suspend fun upsertAll(sessions: List<Session>)
}

/** The one session that has started but not finished, kept so a process death does not lose it. */
interface ActiveSessionStore {
    suspend fun current(): ActiveSession?

    /** Makes [session] the active one, replacing any earlier unfinished session. */
    suspend fun begin(session: ActiveSession)

    /** Drops the active record for [id] without recording a session (for example, one that was too short). */
    suspend fun clear(id: String)
}

/** Sessions saved locally but not yet acknowledged as published to the phone. */
interface SessionOutbox {
    suspend fun pendingIds(): List<String>

    /** Removes [ids] once a publication that included them succeeded. */
    suspend fun acknowledge(ids: List<String>)
}

class RoomSessionRepository @Inject constructor(private val dao: SessionDao) :
    SessionRepository,
    ActiveSessionStore,
    SessionOutbox {
    override fun observeAll(): Flow<List<Session>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeSince(fromEpochMillis: Long): Flow<List<Session>> =
        dao.observeSince(fromEpochMillis).map { list -> list.map { it.toModel() } }

    override suspend fun getSince(fromEpochMillis: Long): List<Session> = dao.getSince(fromEpochMillis).map {
        it.toModel()
    }

    override suspend fun finish(session: Session) = dao.finish(session.toEntity())

    override suspend fun upsertAll(sessions: List<Session>) = dao.upsert(sessions.map { it.toEntity() })

    override suspend fun current(): ActiveSession? = dao.activeSession()?.toModel()

    override suspend fun begin(session: ActiveSession) = dao.begin(session.toEntity())

    override suspend fun clear(id: String) = dao.deleteActive(id)

    override suspend fun pendingIds(): List<String> = dao.pendingIds()

    override suspend fun acknowledge(ids: List<String>) = dao.acknowledge(ids)
}
