package com.codebyramiz.breathefocus.wear

import com.codebyramiz.breathefocus.core.data.ActiveSessionStore
import com.codebyramiz.breathefocus.core.data.SessionOutbox
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.sync.DataLayerCoordinator
import com.codebyramiz.breathefocus.core.data.sync.DataLayerReader
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.model.ActiveSession
import com.codebyramiz.breathefocus.core.model.BreathPhase
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.wear.haptics.Haptics
import com.codebyramiz.breathefocus.wear.health.HeartRate
import com.codebyramiz.breathefocus.wear.health.HeartRateMonitor
import com.codebyramiz.breathefocus.wear.session.ActiveSessions
import com.codebyramiz.breathefocus.wear.session.PhaseAlarms
import com.codebyramiz.breathefocus.wear.session.SessionNotifier
import com.codebyramiz.breathefocus.wear.session.SessionPublisher
import com.codebyramiz.breathefocus.wear.session.SessionRecorder
import com.codebyramiz.breathefocus.wear.session.SessionTicker
import com.codebyramiz.breathefocus.wear.session.SurfaceRefresher
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestCoroutineScheduler

/** Session time follows the test scheduler's virtual clock; advance it with advanceTimeBy. */
class FakeTicker(private val scheduler: TestCoroutineScheduler) : SessionTicker {
    override fun now(): Long = scheduler.currentTime

    var boot = 1

    override fun bootCount(): Int = boot

    override fun frames(): Flow<Long> = flow {
        while (true) {
            emit(now())
            delay(FRAME_MILLIS)
        }
    }

    private companion object {
        const val FRAME_MILLIS = 33L
    }
}

class FakeHaptics : Haptics {
    val phases = mutableListOf<BreathPhase>()
    var blocksFinished = 0

    override fun phaseChanged(phase: BreathPhase) {
        phases += phase
    }

    override fun focusBlockFinished() {
        blocksFinished++
    }
}

class FakeHeartRate : HeartRateMonitor {
    val readings = MutableSharedFlow<HeartRate>(replay = 1)

    override fun readings(): Flow<HeartRate> = readings
}

class FakeNotifier : SessionNotifier {
    var running = false
    var keepAwake = false
    var lastTitle: String? = null

    override fun started(title: String, startedAtElapsedRealtime: Long, keepAwake: Boolean) {
        running = true
        this.keepAwake = keepAwake
        lastTitle = title
    }

    override fun stopped() {
        running = false
    }
}

class FakePhaseAlarms : PhaseAlarms {
    var triggerAt: Long? = null
    var cancelled = 0

    override fun schedule(triggerAtElapsedRealtime: Long) {
        triggerAt = triggerAtElapsedRealtime
    }

    override fun cancel() {
        triggerAt = null
        cancelled++
    }
}

/**
 * In-memory stand-in for the Room repository: one shared state behind the three interfaces, so
 * "finish" has the real semantics of saving, queueing and clearing the active record together.
 */
class FakeSessionStore :
    SessionRepository,
    ActiveSessionStore,
    SessionOutbox {
    val sessions = MutableStateFlow<List<Session>>(emptyList())
    var active: ActiveSession? = null
    val queued = linkedSetOf<String>()

    override fun observeAll(): Flow<List<Session>> = sessions

    override fun observeSince(fromEpochMillis: Long): Flow<List<Session>> =
        sessions.map { list -> list.filter { it.startedAtEpochMillis >= fromEpochMillis } }

    override suspend fun getSince(fromEpochMillis: Long): List<Session> =
        sessions.value.filter { it.startedAtEpochMillis >= fromEpochMillis }

    override suspend fun finish(session: Session) {
        upsertAll(listOf(session))
        queued += session.id
        if (active?.id == session.id) active = null
    }

    override suspend fun upsertAll(sessions: List<Session>) {
        val ids = sessions.map { it.id }.toSet()
        this.sessions.value = this.sessions.value.filterNot { it.id in ids } + sessions
    }

    override suspend fun current(): ActiveSession? = active

    override suspend fun begin(session: ActiveSession) {
        active = session
    }

    override suspend fun clear(id: String) {
        if (active?.id == id) active = null
    }

    override suspend fun pendingIds(): List<String> = queued.toList()

    override suspend fun acknowledge(ids: List<String>) {
        queued -= ids.toSet()
    }
}

class FakeSettings(initial: BreathingPattern = BreathingPattern.DEFAULT) : SettingsRepository {
    val pattern = MutableStateFlow(initial)
    var pending: BreathingPattern? = null
    override val defaultPattern: Flow<BreathingPattern> = pattern

    override suspend fun setDefaultPattern(pattern: BreathingPattern) {
        this.pattern.value = pattern
    }

    override suspend fun selectDefaultPattern(pattern: BreathingPattern) {
        this.pattern.value = pattern
        pending = pattern
    }

    override suspend fun pendingPublication(): BreathingPattern? = pending

    override suspend fun markPublished(pattern: BreathingPattern) {
        if (pending == pattern) pending = null
    }
}

class FakeSync(var fail: Boolean = false) : DataLayerSync {
    val published = mutableListOf<List<Session>>()

    override suspend fun publishSessions(sessions: List<Session>) {
        if (fail) error("phone not connected")
        published += sessions
    }

    override suspend fun publishDefaultPattern(pattern: BreathingPattern) = Unit
}

class FakeDataLayerReader(var pattern: BreathingPattern? = null) : DataLayerReader {
    override suspend fun sessionPayloads(): List<String> = emptyList()

    override suspend fun defaultPattern(): BreathingPattern? = pattern
}

class FakeRefresher : SurfaceRefresher {
    var refreshes = 0

    override fun refresh() {
        refreshes++
    }
}

fun testPublisher(
    store: FakeSessionStore,
    sync: DataLayerSync,
    clock: Clock,
    coordinator: DataLayerCoordinator = DataLayerCoordinator(),
) = SessionPublisher(store, store, sync, coordinator, clock)

fun testRecorder(
    store: FakeSessionStore,
    sync: DataLayerSync,
    clock: Clock,
    refresher: SurfaceRefresher = SurfaceRefresher {},
) = SessionRecorder(store, store, testPublisher(store, sync, clock), refresher)

/** The session lifecycle wired to fakes; the application scope is [scope]. */
fun testActiveSessions(
    store: FakeSessionStore,
    sync: DataLayerSync,
    alarms: PhaseAlarms,
    ticker: SessionTicker,
    clock: Clock,
    scope: CoroutineScope,
    refresher: SurfaceRefresher = SurfaceRefresher {},
) = ActiveSessions(store, testRecorder(store, sync, clock, refresher), alarms, ticker, clock, scope)
