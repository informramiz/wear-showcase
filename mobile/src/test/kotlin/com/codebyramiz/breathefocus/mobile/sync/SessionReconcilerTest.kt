package com.codebyramiz.breathefocus.mobile.sync

import com.codebyramiz.breathefocus.core.data.sync.SyncCodec
import com.codebyramiz.breathefocus.mobile.FakeDataLayerReader
import com.codebyramiz.breathefocus.mobile.FakeSessionRepository
import com.codebyramiz.breathefocus.mobile.session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log on the malformed-payload path.
@RunWith(RobolectricTestRunner::class)
class SessionReconcilerTest {
    private val repo = FakeSessionRepository()
    private val reader = FakeDataLayerReader()
    private val reconciler =
        SessionReconciler(reader, SessionImporter(repo), CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun `imports the chunks stored in the data layer and stays idempotent`() = runTest {
        val a = session("a", 1_000L, minutes = 4)
        val b = session("b", 2_000L, minutes = 6)
        reader.payloads = listOf(SyncCodec.encode(listOf(a)), SyncCodec.encode(listOf(b)))

        reconciler.reconcile()
        reconciler.reconcile()

        assertEquals(setOf(a, b), repo.sessions.value.toSet())
        assertEquals(2, repo.sessions.value.size)
    }

    @Test
    fun `a malformed chunk does not stop the others`() = runTest {
        val good = session("good", 1_000L, minutes = 5)
        reader.payloads = listOf("not json", SyncCodec.encode(listOf(good)))

        reconciler.reconcile()

        assertEquals(listOf(good), repo.sessions.value)
    }
}
