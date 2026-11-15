package com.codebyramiz.breathefocus.wear.ui.focus

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.codebyramiz.breathefocus.wear.R
import com.codebyramiz.breathefocus.wear.ui.components.AmbientTicks
import com.codebyramiz.breathefocus.wear.ui.components.NOTIFICATION_PERMISSION
import com.codebyramiz.breathefocus.wear.ui.components.ambientMinutes
import com.codebyramiz.breathefocus.wear.ui.components.formatClock
import com.codebyramiz.breathefocus.wear.ui.components.hasNotificationPermission
import com.codebyramiz.breathefocus.wear.ui.components.isAmbient
import com.codebyramiz.breathefocus.wear.ui.theme.BreatheTheme

@Composable
fun FocusScreen(onFinished: () -> Unit, viewModel: FocusViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ambient = isAmbient()
    LaunchedEffect(ambient) { viewModel.setAmbient(ambient) }
    AmbientTicks { viewModel.onAmbientTick() }

    // Ask for the notification permission when the user starts, so the Ongoing Activity can
    // show; the session runs either way.
    var notificationsDenied by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsDenied = !it
        viewModel.start()
    }
    FocusContent(
        state = state,
        ambient = ambient,
        notificationsDenied = notificationsDenied,
        onStart = {
            if (hasNotificationPermission(
                    context,
                )
            ) {
                viewModel.start()
            } else {
                notificationLauncher.launch(NOTIFICATION_PERMISSION)
            }
        },
        onStop = {
            viewModel.stop()
            onFinished()
        },
    )
}

@Composable
fun FocusContent(
    state: FocusUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    ambient: Boolean = false,
    notificationsDenied: Boolean = false,
) {
    val pomodoro = state.pomodoro
    val isBreak = pomodoro.phase == FocusPhase.BREAK
    if (ambient) {
        // Mostly black, no animation, minutes only: the ambient tick refreshes it once a minute.
        ScreenScaffold {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${ambientMinutes(pomodoro.remainingMillis)} min", style = MaterialTheme.typography.titleLarge)
                    Text(if (isBreak) "Break" else "Focus", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        return
    }
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
                if (notificationsDenied) {
                    Text(
                        stringResource(R.string.notifications_denied_hint),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.testTag("notifications_hint"),
                    )
                }
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
