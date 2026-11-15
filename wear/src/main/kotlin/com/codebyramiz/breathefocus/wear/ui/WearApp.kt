package com.codebyramiz.breathefocus.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberAmbientModeManager
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
import com.codebyramiz.breathefocus.wear.ui.nav.applyQuickStart
import com.codebyramiz.breathefocus.wear.ui.theme.BreatheTheme

/**
 * @param quickStart pattern requested by the tile: opens straight into a session,
 * with Home underneath so swipe-to-dismiss lands somewhere sensible.
 * @param onQuickStartHandled called once [quickStart] has been applied to the back stack.
 */
@Composable
fun WearApp(quickStart: BreathingPattern? = null, onQuickStartHandled: () -> Unit = {}) {
    // Observing ambient mode at the top level opts the app into the always-on behaviour: the
    // session screens switch to their low-power layout instead of the system dimming the app.
    CompositionLocalProvider(LocalAmbientModeManager provides rememberAmbientModeManager()) {
        BreatheTheme {
            AppScaffold {
                val backStack = rememberNavBackStack(Home)
                LaunchedEffect(quickStart) {
                    if (quickStart != null) {
                        backStack.applyQuickStart(quickStart)
                        onQuickStartHandled()
                    }
                }
                NavDisplay(
                    backStack = backStack,
                    sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
                    // Each entry owns its ViewModels: popping a session screen clears its ViewModel,
                    // which ends and records the session, and the next visit starts fresh.
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
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
}
