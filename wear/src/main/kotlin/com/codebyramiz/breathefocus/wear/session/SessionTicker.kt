package com.codebyramiz.breathefocus.wear.session

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Monotonic session time. [now] anchors and ends a session; [frames] drives animation only
 * while the UI collects it, so nothing ticks at frame rate in the background.
 */
interface SessionTicker {
    /** Monotonic milliseconds on the [SystemClock.elapsedRealtime] time base. */
    fun now(): Long

    /** The device's boot counter; it changes only when the device restarts. 0 when unknown. */
    fun bootCount(): Int

    /** Emits [now] at animation rate for as long as it is collected. */
    fun frames(): Flow<Long>
}

class MonotonicSessionTicker @Inject constructor(@ApplicationContext private val context: Context) : SessionTicker {
    override fun now(): Long = SystemClock.elapsedRealtime()

    override fun bootCount(): Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)

    override fun frames(): Flow<Long> = flow {
        while (true) {
            emit(now())
            delay(FRAME_MILLIS)
        }
    }

    private companion object {
        // ~30 fps is smooth for the ring while staying light on the battery.
        const val FRAME_MILLIS = 33L
    }
}
