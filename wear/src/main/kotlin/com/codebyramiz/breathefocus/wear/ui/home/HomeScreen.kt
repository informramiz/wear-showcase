package com.codebyramiz.breathefocus.wear.ui.home

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.MindfulStats
import com.codebyramiz.breathefocus.wear.ui.history.HistoryViewModel
import com.codebyramiz.breathefocus.wear.ui.theme.BreatheTheme

@Composable
fun HomeScreen(
    onBreathe: (BreathingPattern) -> Unit,
    onFocus: () -> Unit,
    onHistory: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
) {
    val defaultPattern by homeViewModel.defaultPattern.collectAsStateWithLifecycle()
    val history by historyViewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        defaultPattern = defaultPattern,
        todayMinutes = history.todayMinutes,
        onBreathe = onBreathe,
        onFocus = onFocus,
        onHistory = onHistory,
    )
}

@Composable
fun HomeContent(
    defaultPattern: BreathingPattern,
    todayMinutes: Int,
    onBreathe: (BreathingPattern) -> Unit,
    onFocus: () -> Unit,
    onHistory: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = { onBreathe(defaultPattern) }) {
                Text("Start ${defaultPattern.displayName}")
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) {
                    Text(
                        "$todayMinutes / ${MindfulStats.DAILY_GOAL_MINUTES} min today",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            items(BreathingPattern.entries.size) { index ->
                val pattern = BreathingPattern.entries[index]
                HomeButton(
                    label = pattern.displayName,
                    secondary = pattern.summary,
                    icon = if (pattern ==
                        BreathingPattern.COHERENT
                    ) {
                        Icons.Rounded.SelfImprovement
                    } else {
                        Icons.Rounded.Air
                    },
                    onClick = { onBreathe(pattern) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    filled = pattern == defaultPattern,
                )
            }
            item {
                HomeButton(
                    label = "Focus",
                    secondary = "25 min Pomodoro",
                    icon = Icons.Rounded.Timer,
                    onClick = onFocus,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                )
            }
            item {
                HomeButton(
                    label = "History",
                    secondary = null,
                    icon = Icons.Rounded.History,
                    onClick = onHistory,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                )
            }
        }
    }
}

@Composable
private fun HomeButton(
    label: String,
    secondary: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    transformation: SurfaceTransformation,
    filled: Boolean = false,
) {
    val secondaryLabel: (@Composable RowScope.() -> Unit)? = secondary?.let { { Text(it) } }
    val iconSlot: @Composable BoxScope.() -> Unit = { Icon(icon, contentDescription = null) }
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier,
            transformation = transformation,
            label = { Text(label) },
            secondaryLabel = secondaryLabel,
            icon = iconSlot,
        )
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            transformation = transformation,
            label = { Text(label) },
            secondaryLabel = secondaryLabel,
            icon = iconSlot,
            colors = ButtonDefaults.filledTonalButtonColors(),
        )
    }
}

@WearPreviewDevices
@Composable
private fun HomePreview() {
    BreatheTheme {
        HomeContent(BreathingPattern.BOX, 6, {}, {}, {})
    }
}
