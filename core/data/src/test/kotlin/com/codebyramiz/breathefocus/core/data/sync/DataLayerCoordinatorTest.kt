package com.codebyramiz.breathefocus.core.data.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DataLayerCoordinatorTest {
    @Test
    fun `a second publication waits for the first to finish`() = runTest {
        val coordinator = DataLayerCoordinator()
        val log = mutableListOf<String>()
        val release = CompletableDeferred<Unit>()

        launch {
            coordinator.serialized {
                log += "first:start"
                release.await()
                log += "first:end"
            }
        }
        launch { coordinator.serialized { log += "second" } }
        advanceUntilIdle()
        assertEquals(listOf("first:start"), log)

        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("first:start", "first:end", "second"), log)
    }

    @Test
    fun `a failed publication does not block the next one`() = runTest {
        val coordinator = DataLayerCoordinator()
        runCatching { coordinator.serialized { error("boom") } }
        assertEquals("ok", coordinator.serialized { "ok" })
    }
}
