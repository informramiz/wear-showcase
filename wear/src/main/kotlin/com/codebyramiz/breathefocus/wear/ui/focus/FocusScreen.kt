package com.codebyramiz.breathefocus.wear.ui.focus

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.codebyramiz.breathefocus.core.model.FocusPhase
import com.codebyramiz.breathefocus.core.model.Pomodoro
import com.codebyramiz.breathefocus.core.model.PomodoroConfig
import com.codebyramiz.breathefocus.wear.ui.components.formatClock
import com.codebyramiz.breathefocus.wear.ui.theme.BreatheTheme

@Composable
fun FocusScreen(onFinished: () -> Unit, viewModel: FocusViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FocusContent(
        state = state,
        onStart = { viewModel.start(SystemClock.elapsedRealtime()) },
        onStop = {
            viewModel.stop()
            onFinished()
        },
    )
}

@Composable
fun FocusContent(state: FocusUiState, onStart: () -> Unit, onStop: () -> Unit) {
    val pomodoro = state.pomodoro
    val isBreak = pomodoro.phase == FocusPhase.BREAK
    ScreenScaffold {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { pomodoro.progress },
                modifier = Modifier.fillMaxSize().padding(4.dp),
                strokeWidth = 8.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isBreak) "Break" else "Focus",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isBreak) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                )
                Text(
                    formatClock(pomodoro.remainingMillis),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.testTag("focus_remaining"),
                )
                Text("${pomodoro.completedFocusBlocks} blocks done", style = MaterialTheme.typography.labelSmall)
            }
            FilledIconButton(
                onClick = if (state.running) onStop else onStart,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).size(44.dp),
            ) {
                Icon(
                    if (state.running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.running) "Stop" else "Start",
                )
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun FocusPreview() {
    BreatheTheme {
        FocusContent(FocusUiState(true, 7 * 60_000L, Pomodoro.stateAt(PomodoroConfig(), 7 * 60_000L)), {}, {})
    }
}
