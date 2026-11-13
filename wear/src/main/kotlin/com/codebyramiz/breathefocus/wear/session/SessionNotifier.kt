package com.codebyramiz.breathefocus.wear.session

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Starts/stops the Ongoing Activity so a session survives leaving the app. */
interface SessionNotifier {
    fun started(title: String, startedAtElapsedRealtime: Long)

    fun stopped()
}

class OngoingSessionNotifier @Inject constructor(@ApplicationContext private val context: Context) : SessionNotifier {
    override fun started(title: String, startedAtElapsedRealtime: Long) {
        val intent = Intent(context, OngoingSessionService::class.java)
            .putExtra(OngoingSessionService.EXTRA_TITLE, title)
            .putExtra(OngoingSessionService.EXTRA_STARTED_AT, startedAtElapsedRealtime)
        context.startForegroundService(intent)
    }

    override fun stopped() {
        context.stopService(Intent(context, OngoingSessionService::class.java))
    }
}
