package com.codebyramiz.breathefocus.wear.session

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Emits elapsed session time. A fake drives deterministic ViewModel tests. */
fun interface SessionTicker {
    fun elapsedMillis(): Flow<Long>
}

class MonotonicSessionTicker @Inject constructor() : SessionTicker {
    override fun elapsedMillis(): Flow<Long> = flow {
        val start = SystemClock.elapsedRealtime()
        while (true) {
            emit(SystemClock.elapsedRealtime() - start)
            delay(FRAME_MILLIS)
        }
    }

    private companion object {
        // ~30 fps is smooth for the ring while staying light on the battery.
        const val FRAME_MILLIS = 33L
    }
}
