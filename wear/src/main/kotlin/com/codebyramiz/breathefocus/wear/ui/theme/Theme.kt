package com.codebyramiz.breathefocus.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** Calm teal and lavender palette on true black for OLED. */
val BreatheColors = ColorScheme(
    primary = Color(0xFF7FDBCA),
    primaryDim = Color(0xFF5FBFAE),
    primaryContainer = Color(0xFF0E4A43),
    onPrimary = Color(0xFF00201C),
    onPrimaryContainer = Color(0xFFB4F2E6),
    secondary = Color(0xFFC3B5FF),
    secondaryContainer = Color(0xFF3A3170),
    onSecondaryContainer = Color(0xFFE5DEFF),
    tertiary = Color(0xFFFFB4A9),
    background = Color.Black,
)

@Composable
fun BreatheTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BreatheColors, content = content)
}
