package com.codebyramiz.breathefocus.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Same teal and lavender palette as the watch app. */
private val Colors = darkColorScheme(
    primary = Color(0xFF7FDBCA),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF0E4A43),
    onPrimaryContainer = Color(0xFFB4F2E6),
    secondary = Color(0xFFC3B5FF),
    secondaryContainer = Color(0xFF3A3170),
    onSecondaryContainer = Color(0xFFE5DEFF),
    tertiary = Color(0xFFFFB4A9),
    background = Color(0xFF0B0F0E),
    surface = Color(0xFF0B0F0E),
    surfaceContainer = Color(0xFF151C1A),
    surfaceContainerHigh = Color(0xFF1C2523),
)

@Composable
fun BreatheMobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
