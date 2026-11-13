package com.codebyramiz.breathefocus.wear.tile

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.ProgressIndicatorColors
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.model.MindfulStats
import com.codebyramiz.breathefocus.wear.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Today's progress toward the daily goal, plus one tap to start the default breathing pattern. */
// All three arguments are passed on purpose. A bare `Material3TileService()` compiles to
// Kotlin's synthetic default-argument constructor, which Hilt's generated base class does
// not override, so its fields stay uninitialised and onCreate() crashes on injection.
// Dynamic theming is off so the tile keeps the app's teal palette.
@AndroidEntryPoint
class BreatheTileService : Material3TileService(false, BrandTileColors, null) {

    @Inject lateinit var repository: SessionRepository

    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var clock: Clock

    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val today = LocalDate.now(clock)
        val startOfDay = today.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val minutes = MindfulStats(clock.zone).minutesOn(today, repository.observeSince(startOfDay).first())
        val pattern = settings.defaultPattern.first()
        val goal = MindfulStats.DAILY_GOAL_MINUTES
        val layout = primaryLayout(
            titleSlot = { text("Mindful today".layoutString) },
            mainSlot = {
                Box.Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .addContent(
                        circularProgressIndicator(
                            staticProgress = (minutes.toFloat() / goal).coerceIn(0f, 1f),
                            colors = ProgressIndicatorColors(colorScheme.primary, colorScheme.primaryContainer),
                        ),
                    )
                    .addContent(
                        text(
                            "$minutes / $goal".layoutString,
                            typography = Typography.DISPLAY_SMALL,
                            color = colorScheme.onPrimaryContainer,
                        ),
                    )
                    .build()
            },
            bottomSlot = {
                textEdgeButton(onClick = startClickable(pattern.name)) {
                    text("Start ${pattern.displayName}".layoutString)
                }
            },
        )
        return Tile.Builder()
            .setFreshnessIntervalMillis(FRESHNESS_MILLIS)
            .setTileTimeline(Timeline.fromLayoutElement(layout))
            .build()
    }

    private fun startClickable(patternName: String): ModifiersBuilders.Clickable = ModifiersBuilders.Clickable.Builder()
        .setId("start")
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(packageName)
                        .setClassName(ComponentName(this, MainActivity::class.java).className)
                        .addKeyToExtraMapping(
                            MainActivity.EXTRA_PATTERN,
                            ActionBuilders.AndroidStringExtra.Builder().setValue(patternName).build(),
                        )
                        .build(),
                )
                .build(),
        )
        .build()

    private companion object {
        const val FRESHNESS_MILLIS = 30 * 60 * 1000L
    }
}

/** The watch app's palette (see ui/theme/Theme.kt), expressed for ProtoLayout. */
private val BrandTileColors = ColorScheme(
    primary = LayoutColor(0xFF7FDBCA.toInt()),
    primaryDim = LayoutColor(0xFF5FBFAE.toInt()),
    primaryContainer = LayoutColor(0xFF0E4A43.toInt()),
    onPrimary = LayoutColor(0xFF00201C.toInt()),
    onPrimaryContainer = LayoutColor(0xFFB4F2E6.toInt()),
    secondary = LayoutColor(0xFFC3B5FF.toInt()),
    secondaryContainer = LayoutColor(0xFF3A3170.toInt()),
    onSecondaryContainer = LayoutColor(0xFFE5DEFF.toInt()),
    tertiary = LayoutColor(0xFFFFB4A9.toInt()),
)
