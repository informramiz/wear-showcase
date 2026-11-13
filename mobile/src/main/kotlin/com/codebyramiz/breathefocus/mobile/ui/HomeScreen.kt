package com.codebyramiz.breathefocus.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state = state, onPatternSelected = viewModel::selectPattern)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeUiState, onPatternSelected: (BreathingPattern) -> Unit) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Breathe & Focus") }, scrollBehavior = scroll) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { StatsRow(state) }
            item { WeekCard(state) }
            item { PatternCard(state, onPatternSelected) }
            item {
                Text(
                    "Recent sessions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.recent.isEmpty()) {
                item {
                    Text(
                        "Sessions you finish on your watch show up here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("empty_history"),
                    )
                }
            } else {
                items(state.recent, key = { it.id }) { SessionRow(it) }
            }
        }
    }
}

@Composable
private fun StatsRow(state: HomeUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            Icons.Filled.SelfImprovement,
            "${state.todayMinutes} / ${state.goalMinutes}",
            "min today",
            Modifier.weight(1f),
        )
        StatCard(Icons.Filled.LocalFireDepartment, "${state.streakDays}", "day streak", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(top = 8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeekCard(state: HomeUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("This week", style = MaterialTheme.typography.titleMedium)
            Text(
                "${state.weekMinutes} mindful minutes · dashed line is your ${state.goalMinutes}-min goal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(top = 16.dp))
            WeeklyChart(state.week, state.goalMinutes)
        }
    }
}

@Composable
private fun PatternCard(state: HomeUiState, onPatternSelected: (BreathingPattern) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Default pattern on watch", style = MaterialTheme.typography.titleMedium)
            Text(
                state.defaultPattern.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                BreathingPattern.entries.forEach { pattern ->
                    FilterChip(
                        selected = pattern == state.defaultPattern,
                        onClick = { onPatternSelected(pattern) },
                        label = { Text(pattern.displayName) },
                        modifier = Modifier.testTag("pattern_${pattern.name}"),
                    )
                }
            }
            val syncText = when (state.watchSync) {
                WatchSync.Idle -> null
                WatchSync.Sent -> "Sent to your watch"
                WatchSync.Failed -> "Saved. Will reach the watch when it reconnects."
            }
            if (syncText != null) {
                Text(
                    syncText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("sync_status"),
                )
            }
        }
    }
}

private val timeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

@Composable
private fun SessionRow(session: Session) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (session.type == SessionType.FOCUS) Icons.Filled.Timer else Icons.Filled.SelfImprovement,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(session.title(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    timeFormat.format(
                        Instant.ofEpochMilli(session.startedAtEpochMillis).atZone(ZoneId.systemDefault()),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            session.averageHeartRate?.let { bpm ->
                Icon(
                    Icons.Filled.Favorite,
                    null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text("$bpm", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(12.dp))
            }
            Text("${maxOf(1, session.durationMinutes)} min", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun Session.title(): String = when (type) {
    SessionType.FOCUS -> "Focus"
    SessionType.BREATHING -> pattern?.displayName ?: "Breathing"
}
