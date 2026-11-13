package com.codebyramiz.breathefocus.wear.session

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.codebyramiz.breathefocus.wear.MainActivity
import com.codebyramiz.breathefocus.wear.R

/**
 * Foreground service that backs the Ongoing Activity: the session icon on the
 * watch face and in the recents list, with a live stopwatch.
 */
class OngoingSessionService : LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)
        val startedAt = intent?.getLongExtra(EXTRA_STARTED_AT, SystemClock.elapsedRealtime())
            ?: SystemClock.elapsedRealtime()

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_sessions),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_breathe)
            .setContentTitle(title)
            .setContentText(getString(R.string.session_running))
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)

        // Ongoing Activity status parts use the SystemClock.elapsedRealtime() time base.
        val status = Status.Builder()
            .addTemplate("#title# · #time#")
            .addPart("title", Status.TextPart(title))
            .addPart("time", Status.StopwatchPart(startedAt))
            .build()
        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_breathe)
            .setTouchIntent(openApp)
            .setStatus(status)
            .build()
            .apply(applicationContext)

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            builder.build(),
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            } else {
                0
            },
        )
        return START_NOT_STICKY
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_STARTED_AT = "started_at"
        private const val CHANNEL_ID = "sessions"
        private const val NOTIFICATION_ID = 42
    }
}
