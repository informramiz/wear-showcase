package com.codebyramiz.breathefocus.mobile.sync

import android.util.Log
import com.codebyramiz.breathefocus.core.data.sync.DataLayerContract
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Receives session history the watch publishes on /breathe/sessions. */
@AndroidEntryPoint
class MobileListenerService : WearableListenerService() {

    @Inject lateinit var importer: SessionImporter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Copy the payloads out first: the buffer is released when this method returns.
        val payloads = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == DataLayerContract.PATH_SESSIONS }
            .mapNotNull { DataMapItem.fromDataItem(it.dataItem).dataMap.getString(DataLayerContract.KEY_SESSIONS_JSON) }
        val latest = payloads.lastOrNull() ?: return
        scope.launch {
            try {
                importer.import(latest)
            } catch (e: IllegalArgumentException) {
                // kotlinx.serialization's SerializationException is an IllegalArgumentException.
                Log.w(TAG, "Ignoring malformed session payload", e)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MobileListener"
    }
}
