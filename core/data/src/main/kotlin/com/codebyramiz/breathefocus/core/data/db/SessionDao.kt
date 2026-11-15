package com.codebyramiz.breathefocus.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    abstract fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startedAt >= :fromEpochMillis ORDER BY startedAt DESC")
    abstract fun observeSince(fromEpochMillis: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startedAt >= :fromEpochMillis ORDER BY startedAt DESC")
    abstract suspend fun getSince(fromEpochMillis: Long): List<SessionEntity>

    @Upsert
    abstract suspend fun upsert(sessions: List<SessionEntity>)

    @Query("SELECT * FROM active_session LIMIT 1")
    abstract suspend fun activeSession(): ActiveSessionEntity?

    @Query("DELETE FROM active_session")
    protected abstract suspend fun clearActive()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertActive(active: ActiveSessionEntity)

    @Query("DELETE FROM active_session WHERE id = :id")
    abstract suspend fun deleteActive(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun enqueue(entry: OutboxEntity)

    @Query("SELECT sessionId FROM session_outbox ORDER BY rowid")
    abstract suspend fun pendingIds(): List<String>

    @Query("DELETE FROM session_outbox WHERE sessionId IN (:ids)")
    abstract suspend fun acknowledge(ids: List<String>)

    /** Starts [active], replacing any earlier unfinished session. */
    @Transaction
    open suspend fun begin(active: ActiveSessionEntity) {
        clearActive()
        insertActive(active)
    }

    /** Saves a finished session, queues it for the phone and clears its active record, all or nothing. */
    @Transaction
    open suspend fun finish(session: SessionEntity) {
        upsert(listOf(session))
        enqueue(OutboxEntity(session.id))
        deleteActive(session.id)
    }
}
