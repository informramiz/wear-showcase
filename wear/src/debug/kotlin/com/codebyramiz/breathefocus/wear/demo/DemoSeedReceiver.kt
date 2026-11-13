package com.codebyramiz.breathefocus.wear.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.demo.DemoSessions
import com.codebyramiz.breathefocus.wear.session.SurfaceRefresher
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug builds only. Fills the database with a demo week so emulator screenshots have data:
 * `adb shell am broadcast -n com.codebyramiz.breathefocus/com.codebyramiz.breathefocus.wear.demo.DemoSeedReceiver`
 */
@AndroidEntryPoint
class DemoSeedReceiver : BroadcastReceiver() {

    @Inject lateinit var sessions: SessionRepository

    @Inject lateinit var clock: Clock

    @Inject lateinit var surfaces: SurfaceRefresher

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sessions.upsertAll(DemoSessions.generate(clock))
                surfaces.refresh()
            } finally {
                pending.finish()
            }
        }
    }
}
