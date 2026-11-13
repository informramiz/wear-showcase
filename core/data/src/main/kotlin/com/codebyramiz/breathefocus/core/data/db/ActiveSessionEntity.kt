package com.codebyramiz.breathefocus.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codebyramiz.breathefocus.core.model.ActiveSession
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.SessionType

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val pattern: String?,
    val startedAtElapsedRealtime: Long,
    val startedAtEpochMillis: Long,
)

fun ActiveSessionEntity.toModel() = ActiveSession(
    id = id,
    type = SessionType.valueOf(type),
    pattern = pattern?.let { BreathingPattern.fromNameOrDefault(it) },
    startedAtElapsedRealtime = startedAtElapsedRealtime,
    startedAtEpochMillis = startedAtEpochMillis,
)

fun ActiveSession.toEntity() = ActiveSessionEntity(
    id = id,
    type = type.name,
    pattern = pattern?.name,
    startedAtElapsedRealtime = startedAtElapsedRealtime,
    startedAtEpochMillis = startedAtEpochMillis,
)
