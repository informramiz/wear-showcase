package com.codebyramiz.breathefocus.core.data.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow

/** Emits whenever the system date, time or time zone is changed by the user or the network. */
interface TimeChanges {
    val events: Flow<Unit>

    /** For tests and previews that never change the clock. */
    data object None : TimeChanges {
        override val events: Flow<Unit> = emptyFlow()
    }
}

class BroadcastTimeChanges @Inject constructor(@ApplicationContext private val context: Context) : TimeChanges {
    override val events: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate()
}
