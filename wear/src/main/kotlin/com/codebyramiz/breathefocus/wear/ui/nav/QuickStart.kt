package com.codebyramiz.breathefocus.wear.ui.nav

import androidx.navigation3.runtime.NavKey
import com.codebyramiz.breathefocus.core.model.BreathingPattern

/**
 * Applies a quick start from the tile to an existing back stack.
 * A breathing session already on screen is left alone; anything else is replaced by
 * Home with the requested session on top, so swipe-to-dismiss lands on Home.
 */
fun MutableList<NavKey>.applyQuickStart(pattern: BreathingPattern) {
    if (lastOrNull() is Breathing) return
    clear()
    add(Home)
    add(Breathing(pattern.name))
}
