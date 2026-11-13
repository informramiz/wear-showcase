package com.codebyramiz.breathefocus.wear.session

import com.codebyramiz.breathefocus.core.data.ActiveSessionStore
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.model.Session
import javax.inject.Inject

/** Refreshes the tile and complication after the data they show changes. */
fun interface SurfaceRefresher {
    fun refresh()
}

/**
 * One place that finishes a session: persist it atomically with its outbox entry, refresh the
 * watch surfaces and push recent history to the phone. Finishing is idempotent because the
 * session id is the id of the active record it closes.
 */
class SessionRecorder @Inject constructor(
    private val repository: SessionRepository,
    private val activeSessions: ActiveSessionStore,
    private val publisher: SessionPublisher,
    private val refresher: SurfaceRefresher,
) {
    suspend fun record(session: Session) {
        if (session.durationMillis < MIN_DURATION_MILLIS) {
            activeSessions.clear(session.id)
            return
        }
        repository.finish(session)
        refresher.refresh()
        publisher.publishRecent()
    }

    private companion object {
        // Mindful minutes are counted in whole minutes, so shorter sessions add nothing.
        const val MIN_DURATION_MILLIS = 60_000L
    }
}
