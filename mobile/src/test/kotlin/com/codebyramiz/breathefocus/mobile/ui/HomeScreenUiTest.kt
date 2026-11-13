package com.codebyramiz.breathefocus.mobile.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.DayTotal
import com.codebyramiz.breathefocus.mobile.session
import com.codebyramiz.breathefocus.mobile.ui.theme.BreatheMobileTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class HomeScreenUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun showsStatsChartAndPicksPattern() {
        val today = LocalDate.of(2026, 11, 13)
        val state = HomeUiState(
            week = (6 downTo 0).map { DayTotal(today.minusDays(it.toLong()), it * 2) },
            todayMinutes = 7,
            streakDays = 3,
            recent = listOf(session("a", 0L, minutes = 6, hr = 64)),
            defaultPattern = BreathingPattern.BOX,
        )
        var picked: BreathingPattern? = null
        compose.setContent { BreatheMobileTheme { HomeScreen(state) { picked = it } } }

        compose.onNodeWithText("7 / 10").assertIsDisplayed()
        compose.onNodeWithText("3").assertIsDisplayed()
        compose.onNodeWithTag("weekly_chart").assertIsDisplayed()
        compose.onNodeWithTag("pattern_BOX").assertIsSelected()
        compose.onNodeWithTag("pattern_COHERENT").performClick()

        assertEquals(BreathingPattern.COHERENT, picked)
    }
}
