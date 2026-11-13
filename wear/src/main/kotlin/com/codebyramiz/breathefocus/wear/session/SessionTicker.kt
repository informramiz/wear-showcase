package com.codebyramiz.breathefocus.wear.session

import android.os.SystemClock
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

    /** Emits [now] at animation rate for as long as it is collected. */
    fun frames(): Flow<Long>
}

class MonotonicSessionTicker @Inject constructor() : SessionTicker {
    override fun now(): Long = SystemClock.elapsedRealtime()

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
