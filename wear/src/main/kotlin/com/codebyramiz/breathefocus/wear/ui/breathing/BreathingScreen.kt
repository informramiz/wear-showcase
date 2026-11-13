package com.codebyramiz.breathefocus.wear.ui.breathing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.codebyramiz.breathefocus.core.model.BreathPhase
import com.codebyramiz.breathefocus.core.model.BreathingEngine
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.wear.R
import com.codebyramiz.breathefocus.wear.health.HealthServicesHeartRateMonitor
import com.codebyramiz.breathefocus.wear.health.HeartRate
import com.codebyramiz.breathefocus.wear.ui.components.AmbientTicks
import com.codebyramiz.breathefocus.wear.ui.components.NOTIFICATION_PERMISSION
import com.codebyramiz.breathefocus.wear.ui.components.formatClock
import com.codebyramiz.breathefocus.wear.ui.components.hasNotificationPermission
import com.codebyramiz.breathefocus.wear.ui.components.isAmbient
import com.codebyramiz.breathefocus.wear.ui.components.secondsLeft
import com.codebyramiz.breathefocus.wear.ui.theme.BreatheTheme

@Composable
fun BreathingScreen(
    pattern: BreathingPattern,
    onFinished: () -> Unit,
    viewModel: BreathingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ambient = isAmbient()
    LaunchedEffect(ambient) { viewModel.setAmbient(ambient) }
    AmbientTicks { viewModel.onAmbientTick() }

    // Heart rate is optional: the session starts whatever the user answers.
    val heartRateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.start(pattern)
    }
    val startWithHeartRate = {
        if (HealthServicesHeartRateMonitor.hasPermission(context)) {
            viewModel.start(pattern)
        } else {
            heartRateLauncher.launch(HealthServicesHeartRateMonitor.permission)
        }
    }
    // The Ongoing Activity needs the notification permission; the session runs without it.
    var notificationsDenied by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsDenied = !it
        startWithHeartRate()
    }
    LaunchedEffect(pattern) {
        if (hasNotificationPermission(context)) {
            startWithHeartRate()
        } else {
            notificationLauncher.launch(NOTIFICATION_PERMISSION)
        }
    }
    BreathingContent(
        state = state,
        ambient = ambient,
        notificationsDenied = notificationsDenied,
        onStop = {
            viewModel.stop()
            onFinished()
        },
    )
}

@Composable
fun BreathingContent(
    state: BreathingUiState,
    onStop: () -> Unit,
    ambient: Boolean = false,
    notificationsDenied: Boolean = false,
) {
    if (ambient) {
        AmbientBreathing(state)
        return
    }
    val colors = MaterialTheme.colorScheme
    val phaseColor = when (state.breath.phase) {
        BreathPhase.INHALE -> colors.primary
        BreathPhase.EXHALE -> colors.secondary
        BreathPhase.HOLD_IN, BreathPhase.HOLD_OUT -> colors.tertiary
    }
    ScreenScaffold(timeText = {}) {
        Box(Modifier.fillMaxSize().testTag("breathing"), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { state.breath.phaseProgress },
                modifier = Modifier.fillMaxSize().padding(4.dp),
                strokeWidth = 6.dp,
            )
            Canvas(Modifier.fillMaxSize().padding(28.dp)) {
                val maxRadius = size.minDimension / 2f
                val radius = maxRadius * (0.38f + 0.62f * state.breath.ringScale)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(phaseColor.copy(alpha = 0.55f), phaseColor.copy(alpha = 0.08f)),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                )
                drawCircle(color = phaseColor, radius = radius, style = Stroke(width = 2.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatClock(state.elapsedMillis), style = MaterialTheme.typography.labelSmall, color = Color.White)
                Text(
                    state.breath.phase.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.testTag("phase"),
                )
                Text(
                    secondsLeft(state.breath.phaseRemainingMillis).toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                HeartRateRow(state.heartRate)
                if (notificationsDenied) {
                    Text(
                        stringResource(R.string.notifications_denied_hint),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.testTag("notifications_hint"),
                    )
                }
            }
            FilledIconButton(
                onClick = onStop,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).size(40.dp).testTag("stop"),
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = "Stop")
            }
        }
    }
}

/**
 * Ambient layout: mostly black, no animation, and no value that changes faster than the ambient
 * tick, so seconds and heart rate are placeholders.
 */
@Composable
private fun AmbientBreathing(state: BreathingUiState) {
    ScreenScaffold(timeText = {}) {
        Box(Modifier.fillMaxSize().background(Color.Black).testTag("breathing"), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${state.elapsedMillis / 60_000L} min", style = MaterialTheme.typography.titleLarge)
                Text("Breathing", style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("phase"))
                Text("-- bpm", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HeartRateRow(heartRate: HeartRate) {
    val text = when (heartRate) {
        is HeartRate.Bpm -> "${heartRate.value} bpm"
        HeartRate.Acquiring -> "-- bpm"
        is HeartRate.Unavailable -> heartRate.reason
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = null,
            tint = if (heartRate is HeartRate.Bpm) Color(0xFFFF6B6B) else Color.Gray,
            modifier = Modifier.size(14.dp),
        )
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("heart_rate"))
    }
}

@WearPreviewDevices
@Composable
private fun BreathingPreview() {
    BreatheTheme {
        BreathingContent(
            state = BreathingUiState(
                pattern = BreathingPattern.BOX,
                running = true,
                elapsedMillis = 65_000,
                breath = BreathingEngine.stateAt(BreathingPattern.BOX, 1_500),
                heartRate = HeartRate.Bpm(68),
            ),
            onStop = {},
        )
    }
}
