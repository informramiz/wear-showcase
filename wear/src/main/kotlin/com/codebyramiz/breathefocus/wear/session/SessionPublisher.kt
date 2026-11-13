package com.codebyramiz.breathefocus.wear.session

import android.util.Log
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.sync.DataLayerContract
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.data.sync.PendingSessionSync
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pushes recent history to the phone. A failed push is remembered durably and retried on the
 * next session and at the next app start, so the phone catches up once it is reachable.
 */
class SessionPublisher @Inject constructor(
    private val repository: SessionRepository,
    private val sync: DataLayerSync,
    private val pending: PendingSessionSync,
    private val clock: Clock,
) {
    /** @return true if the Data Layer accepted the latest history. */
    suspend fun publishRecent(): Boolean = try {
        val since = clock.millis() - Duration.ofDays(DataLayerContract.SYNC_WINDOW_DAYS).toMillis()
        sync.publishSessions(repository.getSince(since))
        pending.setPending(false)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Session history not published; will retry", e)
        pending.setPending(true)
        false
    }

    /** Retries a push that failed earlier. Does nothing if the phone is already up to date. */
    suspend fun retryIfPending() {
        if (pending.isPending()) publishRecent()
    }

    private companion object {
        const val TAG = "SessionPublisher"
    }
}
