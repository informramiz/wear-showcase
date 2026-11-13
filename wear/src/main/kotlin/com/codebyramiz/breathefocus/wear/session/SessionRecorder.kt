package com.codebyramiz.breathefocus.wear.session

import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.wear.di.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Refreshes the tile and complication after the data they show changes. */
fun interface SurfaceRefresher {
    fun refresh()
}

/**
 * One place that finishes a session: persist it, refresh the watch surfaces and push recent
 * history to the phone.
 */
class SessionRecorder @Inject constructor(
    private val repository: SessionRepository,
    private val publisher: SessionPublisher,
    private val refresher: SurfaceRefresher,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /**
     * Records [session] in the application scope, so it is saved even when the screen that
     * ran it is being torn down (its viewModelScope is already cancelled by then).
     */
    fun finish(session: Session): Job = scope.launch { record(session) }

    suspend fun record(session: Session) {
        if (session.durationMillis < MIN_DURATION_MILLIS) return
        repository.save(session)
        refresher.refresh()
        publisher.publishRecent()
    }

    private companion object {
        // Mindful minutes are counted in whole minutes, so shorter sessions add nothing.
        const val MIN_DURATION_MILLIS = 60_000L
    }
}
