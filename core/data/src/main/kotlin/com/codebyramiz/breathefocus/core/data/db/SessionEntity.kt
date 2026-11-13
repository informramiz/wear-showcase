package com.codebyramiz.breathefocus.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType

@Entity(tableName = "sessions", indices = [Index("startedAt")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val pattern: String?,
    val startedAt: Long,
    val durationMillis: Long,
    val averageHeartRate: Int?,
)

fun SessionEntity.toModel() = Session(
    id = id,
    type = SessionType.valueOf(type),
    pattern = pattern?.let { BreathingPattern.fromNameOrDefault(it) },
    startedAtEpochMillis = startedAt,
    durationMillis = durationMillis,
    averageHeartRate = averageHeartRate,
)

fun Session.toEntity() = SessionEntity(
    id = id,
    type = type.name,
    pattern = pattern?.name,
    startedAt = startedAtEpochMillis,
    durationMillis = durationMillis,
    averageHeartRate = averageHeartRate,
)
