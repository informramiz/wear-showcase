package com.codebyramiz.breathefocus.core.data.sync

import android.net.Uri
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/** Writes shared state to the Wearable Data Layer. Data items sync to every connected node. */
interface DataLayerSync {
    suspend fun publishSessions(sessions: List<Session>)
    suspend fun publishDefaultPattern(pattern: BreathingPattern)
}

class WearableDataLayerSync @Inject constructor(
    private val dataClient: DataClient,
    private val nodeClient: NodeClient,
) : DataLayerSync {

    /**
     * Writes one item per chunk, then deletes chunks left over from a longer earlier history.
     * Each chunk is a self-contained session list, so the phone can import them in any order.
     */
    override suspend fun publishSessions(sessions: List<Session>) {
        val chunks = SyncCodec.encodeChunks(sessions, DataLayerContract.MAX_SESSION_CHUNK_BYTES)
        chunks.forEachIndexed { index, payload ->
            val request = PutDataMapRequest.create(DataLayerContract.sessionChunkPath(index)).apply {
                dataMap.putString(DataLayerContract.KEY_SESSIONS_JSON, payload)
                dataMap.putLong(DataLayerContract.KEY_UPDATED_AT, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
        }
        deleteStaleChunks(keep = chunks.size)
    }

    /** Only this node's own chunks: another watch's history is its own to manage. */
    private suspend fun deleteStaleChunks(keep: Int) {
        val localNodeId = nodeClient.localNode.await().id
        val prefix = Uri.parse("wear://$localNodeId${DataLayerContract.PATH_SESSIONS}")
        val buffer = dataClient.getDataItems(prefix, DataClient.FILTER_PREFIX).await()
        val stale = try {
            buffer.map { it.uri }.filter { uri ->
                val index = DataLayerContract.sessionChunkIndex(uri.path)
                index == null || index >= keep
            }
        } finally {
            buffer.release()
        }
        stale.forEach { dataClient.deleteDataItems(it, DataClient.FILTER_LITERAL).await() }
    }

    override suspend fun publishDefaultPattern(pattern: BreathingPattern) {
        val request = PutDataMapRequest.create(DataLayerContract.PATH_SETTINGS).apply {
            dataMap.putString(DataLayerContract.KEY_DEFAULT_PATTERN, pattern.name)
            dataMap.putLong(DataLayerContract.KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
    }
}
