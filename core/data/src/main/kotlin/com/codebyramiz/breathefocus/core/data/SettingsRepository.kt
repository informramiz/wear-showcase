package com.codebyramiz.breathefocus.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val defaultPattern: Flow<BreathingPattern>

    /** Stores a pattern received from the other device. Nothing is left to publish. */
    suspend fun setDefaultPattern(pattern: BreathingPattern)

    /**
     * Stores a pattern the user chose here and marks it as not yet published, in one write, so a
     * crash cannot leave the choice saved but forgotten.
     */
    suspend fun selectDefaultPattern(pattern: BreathingPattern)

    /** The latest user-chosen pattern the other device has not received yet, or null. */
    suspend fun pendingPublication(): BreathingPattern?

    /** Clears the pending marker if it still names [pattern]; a newer choice stays pending. */
    suspend fun markPublished(pattern: BreathingPattern)
}

class DataStoreSettingsRepository @Inject constructor(private val store: DataStore<Preferences>) : SettingsRepository {
    override val defaultPattern: Flow<BreathingPattern> =
        store.data.map { BreathingPattern.fromNameOrDefault(it[KEY_PATTERN]) }

    override suspend fun setDefaultPattern(pattern: BreathingPattern) {
        store.edit { it[KEY_PATTERN] = pattern.name }
    }

    override suspend fun selectDefaultPattern(pattern: BreathingPattern) {
        store.edit {
            it[KEY_PATTERN] = pattern.name
            it[KEY_PENDING_PATTERN] = pattern.name
        }
    }

    override suspend fun pendingPublication(): BreathingPattern? =
        store.data.first()[KEY_PENDING_PATTERN]?.let { BreathingPattern.fromNameOrDefault(it) }

    override suspend fun markPublished(pattern: BreathingPattern) {
        store.edit { if (it[KEY_PENDING_PATTERN] == pattern.name) it.remove(KEY_PENDING_PATTERN) }
    }

    private companion object {
        val KEY_PATTERN = stringPreferencesKey("default_pattern")
        val KEY_PENDING_PATTERN = stringPreferencesKey("default_pattern_pending")
    }
}
