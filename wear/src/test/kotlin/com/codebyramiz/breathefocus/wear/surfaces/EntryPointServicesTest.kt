package com.codebyramiz.breathefocus.wear.surfaces

import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.codebyramiz.breathefocus.wear.complication.MindfulMinutesComplicationService
import com.codebyramiz.breathefocus.wear.tile.BreatheTileService
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

        val data = runBlocking {
            service.onComplicationRequest(ComplicationRequest(1, ComplicationType.SHORT_TEXT, false))
        }

        val shortText = data as ShortTextComplicationData
        assertEquals("0m", shortText.text.getTextAt(service.resources, Instant.now()).toString())
    }
}
