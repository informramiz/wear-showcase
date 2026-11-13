package com.codebyramiz.breathefocus.wear.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.codebyramiz.breathefocus.core.model.BreathingEngine
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.health.HeartRate
import com.codebyramiz.breathefocus.wear.ui.breathing.BreathingContent
import com.codebyramiz.breathefocus.wear.ui.breathing.BreathingUiState
import com.codebyramiz.breathefocus.wear.ui.history.HistoryContent
import com.codebyramiz.breathefocus.wear.ui.history.HistoryUiState
import com.codebyramiz.breathefocus.wear.ui.theme.BreatheTheme
import java.time.ZoneOffset
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w227dp-h227dp-small-notlong-round-watch-xhdpi-keyshidden-nonav")
class WatchScreensUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun breathingScreenShowsPhaseHeartRateAndStops() {
        var stopped = false
        val state = BreathingUiState(
            pattern = BreathingPattern.BOX,
            running = true,
            elapsedMillis = 5_000,
            breath = BreathingEngine.stateAt(BreathingPattern.BOX, 5_000),
            heartRate = HeartRate.Bpm(64),
        )
        compose.setContent { BreatheTheme { BreathingContent(state, onStop = { stopped = true }) } }

        compose.onNodeWithTag("phase").assertTextContains("Hold", substring = true, ignoreCase = true)
        compose.onNodeWithTag("heart_rate", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("64", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("stop").performClick()
        assertTrue(stopped)
    }

    @Test
    fun historyScreenShowsTodayAndSessions() {
        val state = HistoryUiState(
            loading = false,
            todayMinutes = 7,
            streakDays = 3,
            recent = listOf(Session("1", SessionType.BREATHING, BreathingPattern.BOX, 0, 300_000, 62)),
        )
        compose.setContent { BreatheTheme { HistoryContent(state, ZoneOffset.UTC) } }

        compose.onNodeWithText("7 min today").assertIsDisplayed()
        compose.onNodeWithText("3-day streak").assertIsDisplayed()
        compose.onNodeWithText("62 bpm", substring = true).assertExists()
    }
}
