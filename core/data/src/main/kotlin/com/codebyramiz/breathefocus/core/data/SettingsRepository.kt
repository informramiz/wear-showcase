package com.codebyramiz.breathefocus.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val defaultPattern: Flow<BreathingPattern>
    suspend fun setDefaultPattern(pattern: BreathingPattern)
}

class DataStoreSettingsRepository @Inject constructor(private val store: DataStore<Preferences>) : SettingsRepository {
    override val defaultPattern: Flow<BreathingPattern> =
        store.data.map { BreathingPattern.fromNameOrDefault(it[KEY_PATTERN]) }

    override suspend fun setDefaultPattern(pattern: BreathingPattern) {
        store.edit { it[KEY_PATTERN] = pattern.name }
    }

    private companion object {
        val KEY_PATTERN = stringPreferencesKey("default_pattern")
    }
}
