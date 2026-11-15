package com.codebyramiz.breathefocus.wear.surfaces

import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.codebyramiz.breathefocus.wear.complication.MindfulMinutesComplicationService
import com.codebyramiz.breathefocus.wear.tile.BreatheTileService
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * The system binds these services cold, outside any activity. Creating them through the real
 * Hilt application proves injection works; a constructor that bypasses Hilt's generated base
 * class only shows up here or on a device.
 */
@RunWith(RobolectricTestRunner::class)
class EntryPointServicesTest {

    @Test
    fun `tile service is created with its dependencies injected`() {
        val service = Robolectric.buildService(BreatheTileService::class.java).create().get()
        assertNotNull(service.repository)
    }

    @Test
    fun `complication service answers a request from its injected repository`() {
        val service = Robolectric.buildService(MindfulMinutesComplicationService::class.java).create().get()

        val timeline = runBlocking {
            service.onComplicationRequest(ComplicationRequest(1, ComplicationType.SHORT_TEXT, false))
        }!!

        val today = timeline.defaultComplicationData as ShortTextComplicationData
        assertEquals("0m", today.text.getTextAt(service.resources, Instant.now()).toString())
        // A second entry takes over at the next local midnight, so yesterday's total never lingers.
        val next = timeline.timelineEntries.single()
        assertTrue(next.validity.start.isAfter(Instant.now()))
        assertEquals(Duration.ofDays(1), Duration.between(next.validity.start, next.validity.end))
    }
}
