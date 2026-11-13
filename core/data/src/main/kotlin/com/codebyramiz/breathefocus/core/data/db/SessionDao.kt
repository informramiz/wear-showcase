package com.codebyramiz.breathefocus.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startedAt >= :fromEpochMillis ORDER BY startedAt DESC")
    fun observeSince(fromEpochMillis: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startedAt >= :fromEpochMillis ORDER BY startedAt DESC")
    suspend fun getSince(fromEpochMillis: Long): List<SessionEntity>

    @Upsert
    suspend fun upsert(sessions: List<SessionEntity>)
}
