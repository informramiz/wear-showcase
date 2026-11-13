package com.codebyramiz.breathefocus.mobile.sync

import android.util.Log
import com.codebyramiz.breathefocus.core.data.di.ApplicationScope
import com.codebyramiz.breathefocus.core.data.sync.DataLayerReader
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Imports the session chunks already stored in the Data Layer. Change events can be missed (the
 * listener timed out, the app was not running); the stored items are the source of truth.
 * Imports are upserts, so repeating them is harmless.
 */
@Singleton
class SessionReconciler @Inject constructor(
    private val reader: DataLayerReader,
    private val importer: SessionImporter,
    @ApplicationScope private val scope: CoroutineScope,
) {
    suspend fun reconcile() {
        try {
            reader.sessionPayloads().forEach { importPayload(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not reconcile session history", e)
        }
    }

    /** Reconciles outside the caller, which may be about to be torn down. */
    fun reconcileInBackground(): Job = scope.launch { reconcile() }

    private suspend fun importPayload(payload: String) {
        try {
            importer.import(payload)
        } catch (e: IllegalArgumentException) {
            // kotlinx.serialization's SerializationException is an IllegalArgumentException.
            Log.w(TAG, "Ignoring malformed session payload", e)
        }
    }

    private companion object {
        const val TAG = "SessionReconciler"
    }
}
