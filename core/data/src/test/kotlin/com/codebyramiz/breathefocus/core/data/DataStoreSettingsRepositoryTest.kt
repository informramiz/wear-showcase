package com.codebyramiz.breathefocus.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun TestScope.repository(): DataStoreSettingsRepository {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
            File(tmp.root, "settings.preferences_pb")
        }
        return DataStoreSettingsRepository(store)
    }

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
