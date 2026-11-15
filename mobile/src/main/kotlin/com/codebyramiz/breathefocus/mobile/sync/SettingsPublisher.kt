package com.codebyramiz.breathefocus.mobile.sync

import android.util.Log
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.di.ApplicationScope
import com.codebyramiz.breathefocus.core.data.sync.DataLayerCoordinator
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

/**
 * Sends the pattern chosen on the phone to the watch. The choice is stored with a durable
 * pending marker first, so a failed or interrupted send is retried by [publishPending] at the
 * next app start. Sends run in the application scope, in order, and always carry the latest choice.
 */
@Singleton
class SettingsPublisher @Inject constructor(
    private val settings: SettingsRepository,
    private val sync: DataLayerSync,
    private val coordinator: DataLayerCoordinator,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /**
     * Stores [pattern] and sends it. Leaving the caller (for example a cleared ViewModel) does not
     * cancel the work.
     * @return true if the Data Layer accepted it.
     */
    suspend fun selectAndPublish(pattern: BreathingPattern): Boolean = scope.async {
        settings.selectDefaultPattern(pattern)
        publishPending()
    }.await()

    /** @return true if nothing is left to send. */
    suspend fun publishPending(): Boolean = coordinator.serialized {
        val pending = settings.pendingPublication() ?: return@serialized true
        try {
            sync.publishDefaultPattern(pending)
            settings.markPublished(pending)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Default pattern not published; will retry", e)
            false
        }
    }

    private companion object {
        const val TAG = "SettingsPublisher"
    }
}
