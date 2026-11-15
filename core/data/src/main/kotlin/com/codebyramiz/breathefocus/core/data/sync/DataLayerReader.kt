package com.codebyramiz.breathefocus.core.data.sync

import android.net.Uri
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Reads the items currently stored in the Data Layer. Used to reconcile after a missed or
 * cancelled change event: an item that did not change again produces no new event.
 */
interface DataLayerReader {
    /** The JSON payload of every session chunk the watch has published. */
    suspend fun sessionPayloads(): List<String>

    /** The default pattern most recently published by the phone, if any. */
    suspend fun defaultPattern(): BreathingPattern?
}

class WearableDataLayerReader @Inject constructor(private val dataClient: DataClient) : DataLayerReader {

    override suspend fun sessionPayloads(): List<String> =
        read(DataLayerContract.PATH_SESSIONS, DataClient.FILTER_PREFIX)
            .filter { DataLayerContract.sessionChunkIndex(it.path) != null }
            .mapNotNull { it.map.getString(DataLayerContract.KEY_SESSIONS_JSON) }

    override suspend fun defaultPattern(): BreathingPattern? =
        read(DataLayerContract.PATH_SETTINGS, DataClient.FILTER_LITERAL)
            .maxByOrNull { it.map.getLong(DataLayerContract.KEY_UPDATED_AT) }
            ?.map?.getString(DataLayerContract.KEY_DEFAULT_PATTERN)
            ?.let(BreathingPattern::fromNameOrDefault)

    private class Item(val path: String?, val map: DataMap)

    private suspend fun read(path: String, filter: Int): List<Item> {
        val buffer = dataClient.getDataItems(Uri.parse("wear://*$path"), filter).await()
        return try {
            buffer.map { Item(it.uri.path, DataMapItem.fromDataItem(it).dataMap) }
        } finally {
            buffer.release()
        }
    }
}
