package com.codebyramiz.breathefocus.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved session the phone has not been sent yet. Written in the same transaction as the session. */
@Entity(tableName = "session_outbox")
data class OutboxEntity(@PrimaryKey val sessionId: String)
