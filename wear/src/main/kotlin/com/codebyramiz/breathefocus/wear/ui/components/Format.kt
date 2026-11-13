package com.codebyramiz.breathefocus.wear.ui.components

import java.util.Locale

fun formatClock(millis: Long): String {
    val totalSeconds = millis / 1000
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

/** Seconds left in a phase, rounded up so the count reads 4, 3, 2, 1. */
fun secondsLeft(millis: Long): Long = (millis + 999) / 1000
