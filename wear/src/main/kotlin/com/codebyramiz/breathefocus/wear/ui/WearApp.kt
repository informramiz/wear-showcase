package com.codebyramiz.breathefocus.wear.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.wear.ui.breathing.BreathingScreen
import com.codebyramiz.breathefocus.wear.ui.focus.FocusScreen
import com.codebyramiz.breathefocus.wear.ui.history.HistoryScreen
import com.codebyramiz.breathefocus.wear.ui.home.HomeScreen
import com.codebyramiz.breathefocus.wear.ui.nav.Breathing
import com.codebyramiz.breathefocus.wear.ui.nav.Focus
import com.codebyramiz.breathefocus.wear.ui.nav.History
import com.codebyramiz.breathefocus.wear.ui.nav.Home
import com.codebyramiz.breathefocus.wear.ui.theme.BreatheTheme

/**
 * @param quickStart pattern requested by the tile: opens straight into a session,
 * with Home underneath so swipe-to-dismiss lands somewhere sensible.
 */
@Composable
fun WearApp(quickStart: BreathingPattern? = null) {
    BreatheTheme {
        AppScaffold {
            val initial: Array<NavKey> = if (quickStart != null) arrayOf(Home, Breathing(quickStart.name)) else arrayOf(Home)
            val backStack = rememberNavBackStack(*initial)
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
                entryProvider = entryProvider {
                    entry<Home> {
                        HomeScreen(
                            onBreathe = { backStack.add(Breathing(it.name)) },
                            onFocus = { backStack.add(Focus) },
                            onHistory = { backStack.add(History) },
                        )
                    }
                    entry<Breathing> { key ->
                        BreathingScreen(
                            pattern = BreathingPattern.fromNameOrDefault(key.patternName),
                            onFinished = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<Focus> { FocusScreen(onFinished = { backStack.removeLastOrNull() }) }
                    entry<History> { HistoryScreen() }
                },
            )
        }
    }
}
