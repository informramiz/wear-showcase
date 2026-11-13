package com.codebyramiz.breathefocus.mobile.sync

import com.codebyramiz.breathefocus.core.data.sync.DataLayerCoordinator
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.mobile.FakeSettings
import com.codebyramiz.breathefocus.mobile.FakeSync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log on the failure path.
@RunWith(RobolectricTestRunner::class)
class SettingsPublisherTest {
    private val settings = FakeSettings()
    private val coordinator = DataLayerCoordinator()

    private fun publisher(sync: DataLayerSync) =
        SettingsPublisher(settings, sync, coordinator, CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun `a failed send stays pending and the next start sends it`() = runTest {
        val sync = FakeSync(fail = true)
        val publisher = publisher(sync)

        assertFalse(publisher.selectAndPublish(BreathingPattern.COHERENT))
        assertEquals(BreathingPattern.COHERENT, settings.pending)

        // A new process: the choice survives in the settings, not in memory.
        sync.fail = false
        assertTrue(publisher(sync).publishPending())

        assertEquals(listOf(BreathingPattern.COHERENT), sync.patterns)
        assertNull(settings.pending)
    }

    @Test
    fun `nothing pending sends nothing`() = runTest {
        val sync = FakeSync()

        assertTrue(publisher(sync).publishPending())

        assertEquals(emptyList<BreathingPattern>(), sync.patterns)
    }

    @Test
    fun `a newer choice made during a send is not lost and goes out after it`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val sent = mutableListOf<BreathingPattern>()
        val sync = object : DataLayerSync {
            override suspend fun publishSessions(sessions: List<Session>) = Unit

            override suspend fun publishDefaultPattern(pattern: BreathingPattern) {
                if (sent.isEmpty()) gate.await()
                sent += pattern
            }
        }
        val publisher = publisher(sync)

        val first = launch { publisher.selectAndPublish(BreathingPattern.BOX) }
        val second = launch { publisher.selectAndPublish(BreathingPattern.COHERENT) }
        gate.complete(Unit)
        first.join()
        second.join()

        assertEquals(BreathingPattern.COHERENT, sent.last())
        assertNull(settings.pending)
    }
}
