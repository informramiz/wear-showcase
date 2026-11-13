package com.codebyramiz.breathefocus.wear.ui.nav

import androidx.navigation3.runtime.NavKey
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickStartTest {

    @Test
    fun `tile start from another screen opens the session over Home`() {
        val backStack = mutableListOf<NavKey>(Home, History)

        backStack.applyQuickStart(BreathingPattern.BOX)

        assertEquals(listOf(Home, Breathing(BreathingPattern.BOX.name)), backStack)
    }

    @Test
    fun `tile start leaves a running session alone`() {
        val running = Breathing(BreathingPattern.COHERENT.name)
        val backStack = mutableListOf(Home, running)

        backStack.applyQuickStart(BreathingPattern.BOX)

        assertEquals(listOf(Home, running), backStack)
    }
}
