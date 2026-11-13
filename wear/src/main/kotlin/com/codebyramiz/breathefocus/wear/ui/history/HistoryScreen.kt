package com.codebyramiz.breathefocus.wear.ui.history

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.ui.theme.BreatheTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryContent(state)
}

private val timeFormat = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.US)

@Composable
fun HistoryContent(state: HistoryUiState, zone: ZoneId = ZoneId.systemDefault()) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) {
                    Text("History")
                }
            }
            item {
                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec).testTag("history_summary"),
                    transformation = SurfaceTransformation(spec),
                ) {
                    Text(
                        "${state.todayMinutes} min today",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("${state.streakDays}-day streak", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!state.loading && state.recent.isEmpty()) {
                item {
                    Text(
                        "No sessions yet. Take one minute to breathe.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    )
                }
            }
            items(state.recent.size) { index ->
                val session = state.recent[index]
                TitleCard(
                    onClick = {},
                    title = { Text(session.title()) },
                    time = { Text("${session.durationMinutes}m") },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) {
                    val whenText = timeFormat.format(Instant.ofEpochMilli(session.startedAtEpochMillis).atZone(zone))
                    val hr = session.averageHeartRate?.let { " · $it bpm" }.orEmpty()
                    Text(whenText + hr, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun Session.title(): String = when (type) {
    SessionType.BREATHING -> pattern?.displayName ?: "Breathing"
    SessionType.FOCUS -> "Focus"
}

@WearPreviewDevices
@Composable
private fun HistoryPreview() {
    val now = System.currentTimeMillis()
    BreatheTheme {
        HistoryContent(
            HistoryUiState(
                loading = false,
                todayMinutes = 14,
                streakDays = 3,
                recent = listOf(
                    Session("1", SessionType.BREATHING, BreathingPattern.BOX, now - 3_600_000, 300_000, 66),
                    Session("2", SessionType.FOCUS, null, now - 7_200_000, 1_500_000, null),
                ),
            ),
        )
    }
}
