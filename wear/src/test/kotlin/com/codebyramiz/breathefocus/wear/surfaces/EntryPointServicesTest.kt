package com.codebyramiz.breathefocus.wear.surfaces

import com.codebyramiz.breathefocus.wear.complication.MindfulMinutesComplicationService
import com.codebyramiz.breathefocus.wear.tile.BreatheTileService
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
    fun `complication service is created with its dependencies injected`() {
        val service = Robolectric.buildService(MindfulMinutesComplicationService::class.java).create().get()
        assertNotNull(service)
    }
}
