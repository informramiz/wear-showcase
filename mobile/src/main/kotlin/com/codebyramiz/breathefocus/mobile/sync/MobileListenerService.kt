package com.codebyramiz.breathefocus.mobile.sync

import android.util.Log
import com.codebyramiz.breathefocus.core.data.sync.DataLayerContract
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/** Receives the session history chunks the watch publishes under /breathe/sessions/. */
@AndroidEntryPoint
class MobileListenerService : WearableListenerService() {

    @Inject lateinit var importer: SessionImporter

    @Inject lateinit var reconciler: SessionReconciler

    /**
     * Runs on a background thread. The import completes before returning: the system may unbind
     * the service as soon as this callback ends, which would cancel work launched elsewhere.
     */
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Each chunk is a self-contained session list; upserts make the order irrelevant.
        val payloads = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED && isSessionItem(it.dataItem.uri.path) }
            .mapNotNull { DataMapItem.fromDataItem(it.dataItem).dataMap.getString(DataLayerContract.KEY_SESSIONS_JSON) }
        if (payloads.isEmpty()) return
        val imported = runBlocking { withTimeoutOrNull(IMPORT_TIMEOUT) { payloads.forEach { importChunk(it) } } }
        if (imported == null) {
            Log.w(TAG, "Timed out importing session history from the watch; reconciling")
            reconciler.reconcileInBackground()
        }
    }

    private suspend fun importChunk(payload: String) {
        try {
            importer.import(payload)
        } catch (e: IllegalArgumentException) {
            // kotlinx.serialization's SerializationException is an IllegalArgumentException.
            Log.w(TAG, "Ignoring malformed session payload", e)
        }
    }

    private fun isSessionItem(path: String?): Boolean = DataLayerContract.sessionChunkIndex(path) != null

    private companion object {
        const val TAG = "MobileListener"
        val IMPORT_TIMEOUT = 10.seconds
    }
}
