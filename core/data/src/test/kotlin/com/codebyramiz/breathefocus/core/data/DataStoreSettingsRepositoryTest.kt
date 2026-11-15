package com.codebyramiz.breathefocus.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * In-memory store: DataStore's file-based atomic rename is unreliable on Windows hosts (the CI runner),
 * and these tests cover the repository's logic, not DataStore's file handling.
 */
private class InMemoryPreferencesStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

class DataStoreSettingsRepositoryTest {
    private fun repository() = DataStoreSettingsRepository(InMemoryPreferencesStore())

    @Test
    fun `defaults to box and persists a new choice`() = runTest {
        val repo = repository()
        assertEquals(BreathingPattern.BOX, repo.defaultPattern.first())
        repo.setDefaultPattern(BreathingPattern.COHERENT)
        assertEquals(BreathingPattern.COHERENT, repo.defaultPattern.first())
        assertNull(repo.pendingPublication())
    }

    @Test
    fun `a user choice is saved together with its pending publication`() = runTest {
        val repo = repository()
        repo.selectDefaultPattern(BreathingPattern.COHERENT)
        assertEquals(BreathingPattern.COHERENT, repo.defaultPattern.first())
        assertEquals(BreathingPattern.COHERENT, repo.pendingPublication())
    }

    @Test
    fun `marking published clears only the matching choice`() = runTest {
        val repo = repository()
        repo.selectDefaultPattern(BreathingPattern.COHERENT)
        repo.selectDefaultPattern(BreathingPattern.BOX)
        repo.markPublished(BreathingPattern.COHERENT)
        assertEquals(BreathingPattern.BOX, repo.pendingPublication())
        repo.markPublished(BreathingPattern.BOX)
        assertNull(repo.pendingPublication())
    }
}
