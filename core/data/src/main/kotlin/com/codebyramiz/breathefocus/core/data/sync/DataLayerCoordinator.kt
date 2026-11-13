package com.codebyramiz.breathefocus.core.data.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes every Data Layer publication in the app. Chunk paths are shared, so two overlapping
 * publications could let an older snapshot overwrite or delete newer chunks.
 */
@Singleton
class DataLayerCoordinator @Inject constructor() {
    private val mutex = Mutex()

    suspend fun <T> serialized(block: suspend () -> T): T = mutex.withLock { block() }
}
