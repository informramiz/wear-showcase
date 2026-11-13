package com.codebyramiz.breathefocus.core.data.sync

import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/** Writes shared state to the Wearable Data Layer. Data items sync to every connected node. */
interface DataLayerSync {
    suspend fun publishSessions(sessions: List<Session>)
    suspend fun publishDefaultPattern(pattern: BreathingPattern)
}

class WearableDataLayerSync @Inject constructor(private val dataClient: DataClient) : DataLayerSync {

    override suspend fun publishSessions(sessions: List<Session>) {
        val request = PutDataMapRequest.create(DataLayerContract.PATH_SESSIONS).apply {
            dataMap.putString(DataLayerContract.KEY_SESSIONS_JSON, SyncCodec.encode(sessions))
            dataMap.putLong(DataLayerContract.KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
    }

    override suspend fun publishDefaultPattern(pattern: BreathingPattern) {
        val request = PutDataMapRequest.create(DataLayerContract.PATH_SETTINGS).apply {
            dataMap.putString(DataLayerContract.KEY_DEFAULT_PATTERN, pattern.name)
            dataMap.putLong(DataLayerContract.KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
    }
}
