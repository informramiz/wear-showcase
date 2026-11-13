package com.codebyramiz.breathefocus.wear.sync

import com.codebyramiz.breathefocus.core.data.sync.DataLayerReader
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.wear.FakeDataLayerReader
import com.codebyramiz.breathefocus.wear.FakeRefresher
import com.codebyramiz.breathefocus.wear.FakeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log on the failure path.
@RunWith(RobolectricTestRunner::class)
class DefaultPatternImporterTest {
    private val settings = FakeSettings(BreathingPattern.BOX)
    private val refresher = FakeRefresher()

    private fun importer(reader: DataLayerReader = FakeDataLayerReader()) =
        DefaultPatternImporter(settings, reader, refresher, CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun `a new pattern is saved and the tile is refreshed`() = runTest {
        importer().import(BreathingPattern.COHERENT)

        assertEquals(BreathingPattern.COHERENT, settings.pattern.value)
        assertEquals(1, refresher.refreshes)
    }

    @Test
    fun `importing the same pattern again changes nothing`() = runTest {
        val importer = importer()

        importer.import(BreathingPattern.COHERENT)
        importer.import(BreathingPattern.COHERENT)

        assertEquals(1, refresher.refreshes)
    }

    @Test
    fun `reconcile applies the pattern stored in the data layer`() = runTest {
        importer(FakeDataLayerReader(BreathingPattern.RELAX_478)).reconcile()

        assertEquals(BreathingPattern.RELAX_478, settings.pattern.value)
        assertEquals(1, refresher.refreshes)
    }

    @Test
    fun `reconcile with nothing stored leaves the setting alone`() = runTest {
        importer().reconcile()

        assertEquals(BreathingPattern.BOX, settings.pattern.value)
        assertEquals(0, refresher.refreshes)
    }

    @Test
    fun `reconcile survives an unreachable data layer`() = runTest {
        val broken = object : DataLayerReader {
            override suspend fun sessionPayloads(): List<String> = error("no connection")

            override suspend fun defaultPattern(): BreathingPattern? = error("no connection")
        }

        importer(broken).reconcile()

        assertEquals(BreathingPattern.BOX, settings.pattern.value)
    }
}
