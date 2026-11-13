package com.codebyramiz.breathefocus.wear.session

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Starts/stops the Ongoing Activity so a session survives leaving the app. */
interface SessionNotifier {
    /**
     * @param keepAwake hold the CPU awake for a bounded time so timed cues still fire with the
     * screen off. Sessions whose cues are scheduled with alarms do not need it.
     */
    fun started(title: String, startedAtElapsedRealtime: Long, keepAwake: Boolean = false)

    fun stopped()
}

class OngoingSessionNotifier @Inject constructor(@ApplicationContext private val context: Context) : SessionNotifier {
    override fun started(title: String, startedAtElapsedRealtime: Long, keepAwake: Boolean) {
        val intent = Intent(context, OngoingSessionService::class.java)
            .putExtra(OngoingSessionService.EXTRA_TITLE, title)
            .putExtra(OngoingSessionService.EXTRA_STARTED_AT, startedAtElapsedRealtime)
            .putExtra(OngoingSessionService.EXTRA_KEEP_AWAKE, keepAwake)
        context.startForegroundService(intent)
    }

    override fun stopped() {
        context.stopService(Intent(context, OngoingSessionService::class.java))
    }
}
