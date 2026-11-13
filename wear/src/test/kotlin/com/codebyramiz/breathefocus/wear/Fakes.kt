package com.codebyramiz.breathefocus.wear

import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.data.sync.PendingSessionSync
import com.codebyramiz.breathefocus.core.model.BreathPhase
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.core.model.Session
import com.codebyramiz.breathefocus.wear.haptics.Haptics
import com.codebyramiz.breathefocus.wear.health.HeartRate
import com.codebyramiz.breathefocus.wear.health.HeartRateMonitor
import com.codebyramiz.breathefocus.wear.session.SessionNotifier
import com.codebyramiz.breathefocus.wear.session.SessionPublisher
import com.codebyramiz.breathefocus.wear.session.SessionRecorder
import com.codebyramiz.breathefocus.wear.session.SessionTicker
import com.codebyramiz.breathefocus.wear.session.SurfaceRefresher
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
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
    var lastTitle: String? = null

    override fun started(title: String, startedAtElapsedRealtime: Long) {
        running = true
        lastTitle = title
    }

    override fun stopped() {
        running = false
    }
}

class FakeSessionRepository : SessionRepository {
    val sessions = MutableStateFlow<List<Session>>(emptyList())

    override fun observeAll(): Flow<List<Session>> = sessions

    override fun observeSince(fromEpochMillis: Long): Flow<List<Session>> =
        sessions.map { list -> list.filter { it.startedAtEpochMillis >= fromEpochMillis } }

    override suspend fun getSince(fromEpochMillis: Long): List<Session> =
        sessions.value.filter { it.startedAtEpochMillis >= fromEpochMillis }

    override suspend fun save(session: Session) {
        sessions.value = sessions.value.filterNot { it.id == session.id } + session
    }

    override suspend fun upsertAll(sessions: List<Session>) = sessions.forEach { save(it) }
}

class FakeSettings(initial: BreathingPattern = BreathingPattern.DEFAULT) : SettingsRepository {
    private val pattern = MutableStateFlow(initial)
    override val defaultPattern: Flow<BreathingPattern> = pattern

    override suspend fun setDefaultPattern(pattern: BreathingPattern) {
        this.pattern.value = pattern
    }
}

class FakePendingSync(var pending: Boolean = false) : PendingSessionSync {
    override suspend fun isPending(): Boolean = pending

    override suspend fun setPending(pending: Boolean) {
        this.pending = pending
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

/** A recorder wired to fakes; records on [dispatcher] like the real application scope. */
fun testRecorder(
    repository: FakeSessionRepository,
    sync: FakeSync,
    clock: Clock,
    dispatcher: CoroutineDispatcher,
    refresher: SurfaceRefresher = SurfaceRefresher {},
) = SessionRecorder(
    repository,
    SessionPublisher(repository, sync, FakePendingSync(), clock),
    refresher,
    CoroutineScope(dispatcher),
)
