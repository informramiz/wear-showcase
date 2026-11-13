package com.codebyramiz.breathefocus.wear.session

import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.sync.DataLayerContract
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.model.Session
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/** Refreshes the tile and complication after the data they show changes. */
fun interface SurfaceRefresher {
    fun refresh()
}

/**
 * One place that finishes a session: persist it, push the last 30 days to the
 * phone over the Data Layer, and refresh the watch surfaces.
 */
class SessionRecorder @Inject constructor(
    private val repository: SessionRepository,
    private val sync: DataLayerSync,
    private val refresher: SurfaceRefresher,
    private val clock: Clock,
) {
    suspend fun record(session: Session) {
        if (session.durationMillis < MIN_DURATION_MILLIS) return
        repository.save(session)
        refresher.refresh()
        val since = clock.millis() - Duration.ofDays(DataLayerContract.SYNC_WINDOW_DAYS.toLong()).toMillis()
        // The phone may be off or unpaired: history stays on the watch and syncs next time.
        runCatching { sync.publishSessions(repository.getSince(since)) }
    }

    private companion object {
        const val MIN_DURATION_MILLIS = 10_000L
    }
}
