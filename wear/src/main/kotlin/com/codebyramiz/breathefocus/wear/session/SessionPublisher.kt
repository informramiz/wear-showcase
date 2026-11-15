package com.codebyramiz.breathefocus.wear.session

import android.util.Log
import com.codebyramiz.breathefocus.core.data.SessionOutbox
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.sync.DataLayerContract
import com.codebyramiz.breathefocus.core.data.sync.DataLayerCoordinator
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pushes recent history to the phone. Finished sessions wait in a durable outbox until a push
 * that included them succeeds, so an interruption at any point is retried at the next session
 * or app start. All pushes go through [DataLayerCoordinator], so they never interleave.
 */
class SessionPublisher @Inject constructor(
    private val repository: SessionRepository,
    private val outbox: SessionOutbox,
    private val sync: DataLayerSync,
    private val coordinator: DataLayerCoordinator,
    private val clock: Clock,
) {
    /** @return true if the Data Layer accepted the latest history. */
    suspend fun publishRecent(): Boolean = coordinator.serialized {
        try {
            val queued = outbox.pendingIds()
            val since = clock.millis() - Duration.ofDays(DataLayerContract.SYNC_WINDOW_DAYS).toMillis()
            val recent = repository.getSince(since)
            sync.publishSessions(recent)
            // Only sessions that were in this snapshot are done; one older than the sync window stays queued.
            val published = recent.mapTo(HashSet()) { it.id }
            outbox.acknowledge(queued.filter { it in published })
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Session history not published; will retry", e)
            false
        }
    }

    /** Retries a push that failed earlier. Does nothing if the phone is already up to date. */
    suspend fun retryIfPending() {
        if (outbox.pendingIds().isNotEmpty()) publishRecent()
    }

    private companion object {
        const val TAG = "SessionPublisher"
    }
}
