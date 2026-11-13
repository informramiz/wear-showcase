package com.codebyramiz.breathefocus.core.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Durable "the phone has not received the latest history yet" marker, kept across restarts. */
interface PendingSessionSync {
    suspend fun isPending(): Boolean
    suspend fun setPending(pending: Boolean)
}

class DataStorePendingSessionSync @Inject constructor(private val store: DataStore<Preferences>) :
    PendingSessionSync {
    override suspend fun isPending(): Boolean = store.data.first()[KEY_PENDING] ?: false

    override suspend fun setPending(pending: Boolean) {
        store.edit { it[KEY_PENDING] = pending }
    }

    private companion object {
        val KEY_PENDING = booleanPreferencesKey("sessions_sync_pending")
    }
}
