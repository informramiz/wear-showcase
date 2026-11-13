package com.codebyramiz.breathefocus.wear.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.AmbientTickEffect
import androidx.wear.compose.foundation.LocalAmbientModeManager

/** True while the watch is in ambient (always-on, low power) mode. */
@Composable
fun isAmbient(): Boolean = LocalAmbientModeManager.current?.currentAmbientMode is AmbientMode.Ambient

/** Runs [onTick] about once a minute while the watch is in ambient mode. */
@Composable
fun AmbientTicks(onTick: () -> Unit) {
    LocalAmbientModeManager.current?.AmbientTickEffect(onTick)
}

/** Ongoing Activities need the notification permission on Wear OS 4 and later. */
fun hasNotificationPermission(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED

// The constant is inlined at compile time, and the permission is only requested on API 33+.
@SuppressLint("InlinedApi")
const val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

/** Whole minutes for ambient mode, which must not show seconds. Rounds up like a countdown. */
fun ambientMinutes(millis: Long): Long = (millis + 59_999L) / 60_000L
