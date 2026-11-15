package com.codebyramiz.breathefocus.wear.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.codebyramiz.breathefocus.core.data.ActiveSessionStore
import com.codebyramiz.breathefocus.core.data.di.ApplicationScope
import com.codebyramiz.breathefocus.core.model.ActiveSession
import com.codebyramiz.breathefocus.core.model.Pomodoro
import com.codebyramiz.breathefocus.core.model.PomodoroConfig
import com.codebyramiz.breathefocus.core.model.SessionType
import com.codebyramiz.breathefocus.wear.haptics.Haptics
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** A wake-up call at a moment on the [android.os.SystemClock.elapsedRealtime] time base. */
interface PhaseAlarms {
    /** [sessionId] travels with the alarm so a late delivery for an earlier session can be ignored. */
    fun schedule(triggerAtElapsedRealtime: Long, sessionId: String = "")

    fun cancel()
}

/**
 * Wakeup alarms rather than coroutine delays: a delay follows the uptime clock, which stops in
 * deep sleep, so a screen-off Pomodoro boundary would arrive late. Exact timing needs the
 * user-granted SCHEDULE_EXACT_ALARM; without it the alarm is inexact but still wakes the device.
 */
class AlarmManagerPhaseAlarms @Inject constructor(@ApplicationContext private val context: Context) : PhaseAlarms {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private fun pendingIntent(sessionId: String = "") = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, PhaseAlarmReceiver::class.java).putExtra(EXTRA_SESSION_ID, sessionId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    override fun schedule(triggerAtElapsedRealtime: Long, sessionId: String) {
        // Exact alarms need no grant before API 31.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtElapsedRealtime,
                pendingIntent(sessionId),
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtElapsedRealtime,
                pendingIntent(sessionId),
            )
        }
    }

    override fun cancel() = alarmManager.cancel(pendingIntent())

    companion object {
        const val EXTRA_SESSION_ID = "sessionId"
    }
}

/** Buzzes at each Pomodoro boundary of the persisted focus session and arms the next one. */
@Singleton
class PomodoroBoundaries @Inject constructor(
    private val store: ActiveSessionStore,
    private val alarms: PhaseAlarms,
    private val haptics: Haptics,
    private val ticker: SessionTicker,
) {
    private val config = PomodoroConfig()

    /** Arms the alarm for the end of the block [session] is in. */
    fun scheduleNext(session: ActiveSession) {
        val now = ticker.now()
        val elapsed = now - session.startedAtElapsedRealtime
        alarms.schedule(now + Pomodoro.stateAt(config, elapsed).remainingMillis, session.id)
    }

    /** [expectedSessionId] is the session the alarm was armed for; an alarm for any other session is dropped. */
    suspend fun onAlarm(expectedSessionId: String? = null) {
        val session = store.current()?.takeIf { it.type == SessionType.FOCUS } ?: return
        if (expectedSessionId != null && session.id != expectedSessionId) return
        haptics.focusBlockFinished()
        scheduleNext(session)
    }
}

@AndroidEntryPoint
class PhaseAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var boundaries: PomodoroBoundaries

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                boundaries.onAlarm(intent.getStringExtra(AlarmManagerPhaseAlarms.EXTRA_SESSION_ID))
            } finally {
                pending.finish()
            }
        }
    }
}
