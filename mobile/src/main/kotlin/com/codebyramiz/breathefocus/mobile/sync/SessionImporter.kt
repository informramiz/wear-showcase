package com.codebyramiz.breathefocus.mobile.sync

import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.sync.SyncCodec
import javax.inject.Inject

/** Turns a session payload from the watch into local rows. Upsert keeps repeated syncs idempotent. */
class SessionImporter @Inject constructor(private val sessions: SessionRepository) {
    suspend fun import(payload: String): Int {
        val decoded = SyncCodec.decode(payload)
        sessions.upsertAll(decoded)
        return decoded.size
    }
}
