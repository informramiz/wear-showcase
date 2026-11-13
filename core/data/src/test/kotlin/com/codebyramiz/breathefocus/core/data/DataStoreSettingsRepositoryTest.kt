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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `defaults to box and persists a new choice`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
            File(tmp.root, "settings.preferences_pb")
        }
        val repo = DataStoreSettingsRepository(store)
        assertEquals(BreathingPattern.BOX, repo.defaultPattern.first())
        repo.setDefaultPattern(BreathingPattern.COHERENT)
        assertEquals(BreathingPattern.COHERENT, repo.defaultPattern.first())
    }
}
