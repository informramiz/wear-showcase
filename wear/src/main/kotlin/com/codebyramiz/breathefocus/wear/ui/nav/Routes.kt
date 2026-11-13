package com.codebyramiz.breathefocus.wear.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

/** The pattern travels by name: core:model stays free of serialization. */
@Serializable
data class Breathing(val patternName: String) : NavKey

@Serializable
data object Focus : NavKey

@Serializable
data object History : NavKey
