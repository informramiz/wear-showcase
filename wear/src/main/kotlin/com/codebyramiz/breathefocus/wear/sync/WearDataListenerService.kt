package com.codebyramiz.breathefocus.wear.sync

import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.sync.DataLayerContract
import com.codebyramiz.breathefocus.core.model.BreathingPattern
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

/** Receives the default pattern the user picked on the phone. */
@AndroidEntryPoint
class WearDataListenerService : WearableListenerService() {

    @Inject lateinit var settings: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Read everything out before returning: the buffer is released afterwards.
        val patterns = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == DataLayerContract.PATH_SETTINGS }
            .mapNotNull {
                DataMapItem.fromDataItem(it.dataItem).dataMap.getString(DataLayerContract.KEY_DEFAULT_PATTERN)
            }
        val latest = patterns.lastOrNull() ?: return
        scope.launch { settings.setDefaultPattern(BreathingPattern.fromNameOrDefault(latest)) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
