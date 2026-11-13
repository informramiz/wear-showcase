package com.codebyramiz.breathefocus.core.model

enum class SessionType { BREATHING, FOCUS }

/**
 * A finished mindfulness session.
 *
 * @property id stable id (watch-generated) so syncing is an idempotent upsert.
 * @property pattern only set for [SessionType.BREATHING].
 */
data class Session(
    val id: String,
    val type: SessionType,
    val pattern: BreathingPattern?,
    val startedAtEpochMillis: Long,
    val durationMillis: Long,
    val averageHeartRate: Int?,
) {
    val durationMinutes: Int get() = (durationMillis / 60_000L).toInt()
}
