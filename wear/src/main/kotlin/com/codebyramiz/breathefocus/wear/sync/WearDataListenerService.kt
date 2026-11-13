package com.codebyramiz.breathefocus.wear.sync

import android.util.Log
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.sync.DataLayerContract
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/** Receives the default pattern the user picked on the phone. */
@AndroidEntryPoint
class WearDataListenerService : WearableListenerService() {

    @Inject lateinit var settings: SettingsRepository

    /**
     * Runs on a background thread. The save completes before returning: the system may unbind
     * the service as soon as this callback ends, which would cancel work launched elsewhere.
     */
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val latest = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == DataLayerContract.PATH_SETTINGS }
            .mapNotNull {
                DataMapItem.fromDataItem(it.dataItem).dataMap.getString(DataLayerContract.KEY_DEFAULT_PATTERN)
            }
            .lastOrNull() ?: return
        val saved = runBlocking {
            withTimeoutOrNull(SAVE_TIMEOUT) { settings.setDefaultPattern(BreathingPattern.fromNameOrDefault(latest)) }
        }
        if (saved == null) Log.w(TAG, "Timed out saving the default pattern from the phone")
    }

    private companion object {
        const val TAG = "WearDataListener"
        val SAVE_TIMEOUT = 10.seconds
    }
}
