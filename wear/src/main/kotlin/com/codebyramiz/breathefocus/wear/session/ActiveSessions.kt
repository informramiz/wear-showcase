package com.codebyramiz.breathefocus.wear.session

import com.codebyramiz.breathefocus.core.data.ActiveSessionStore
import com.codebyramiz.breathefocus.core.data.di.ApplicationScope
import com.codebyramiz.breathefocus.core.model.ActiveSession
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Pomodoro
import com.codebyramiz.breathefocus.core.model.PomodoroConfig
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.core.model.SessionType
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the durable lifecycle of the one running session. Every operation takes the same lock,
 * so a session recovered after process death is always finished before a new one begins, and
 * finishing never races with recovery.
 */
@Singleton
class ActiveSessions @Inject constructor(
    private val store: ActiveSessionStore,
    private val recorder: SessionRecorder,
    private val alarms: PhaseAlarms,
    private val ticker: SessionTicker,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val lock = Mutex()

    /**
     * Continues the persisted session of [type] if there is one (the process was recreated while
     * it ran), otherwise begins a new one. A persisted session of another type is finished first.
     */
    suspend fun resumeOrBegin(type: SessionType, pattern: BreathingPattern?): ActiveSession = lock.withLock {
        val existing = store.current()
        if (existing != null && existing.type == type) return@withLock existing
        existing?.let { recorder.record(it.recovered()) }
        ActiveSession(
            id = UUID.randomUUID().toString(),
            type = type,
            pattern = pattern,
            startedAtElapsedRealtime = ticker.now(),
            startedAtEpochMillis = clock.millis(),
        ).also { store.begin(it) }
    }

    /** The persisted session of [type], without starting one. */
    suspend fun current(type: SessionType): ActiveSession? = lock.withLock {
        store.current()?.takeIf { it.type == type }
    }

    /**
     * Finishes [session] in the application scope, so it is saved even when the screen that ran
     * it is being torn down. Calls are queued in the order they are made.
     */
    fun finish(session: Session): Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        lock.withLock {
            alarms.cancel()
            recorder.record(session)
        }
    }

    /** Finishes a session left behind by a process that died, using the time it had when last seen. */
    fun recoverOrphan(): Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        lock.withLock {
            store.current()?.let {
                alarms.cancel()
                recorder.record(it.recovered())
            }
        }
    }

    private fun ActiveSession.recovered(): Session {
        val elapsed = recoveredElapsedMillis(
            monotonicMillis = ticker.now() - startedAtElapsedRealtime,
            wallClockMillis = clock.millis() - startedAtEpochMillis,
        )
        val counted = if (type == SessionType.FOCUS) Pomodoro.focusedMillis(PomodoroConfig(), elapsed) else elapsed
        return Session(
            id = id,
            type = type,
            pattern = pattern,
            startedAtEpochMillis = startedAtEpochMillis,
            durationMillis = counted,
            averageHeartRate = null,
        )
    }

    internal companion object {
        /** Longest a recovered session may count, so a long-dead process does not credit hours. */
        const val MAX_RECOVERED_MILLIS = 2 * 60 * 60_000L
        private const val CLOCK_DISAGREEMENT_MILLIS = 60_000L

        /**
         * The monotonic clock restarts at reboot, so a disagreement with the wall clock means the
         * device restarted and only the wall clock is meaningful.
         */
        fun recoveredElapsedMillis(monotonicMillis: Long, wallClockMillis: Long): Long {
            val rebooted = monotonicMillis < 0 || abs(monotonicMillis - wallClockMillis) > CLOCK_DISAGREEMENT_MILLIS
            val elapsed = if (rebooted) wallClockMillis else monotonicMillis
            return elapsed.coerceIn(0, MAX_RECOVERED_MILLIS)
        }
    }
}
