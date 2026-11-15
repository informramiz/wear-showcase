package com.codebyramiz.breathefocus.wear.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The clock a session screen renders from. Interactive, it follows the animation ticker; in
 * ambient mode the ticker is suspended and a new value arrives only with each ambient tick
 * (about once a minute), as the always-on guidance asks.
 */
class SessionFrames(private val ticker: SessionTicker) {
    private val ambient = MutableStateFlow(false)
    private val ambientTicks = MutableStateFlow(0)

    fun setAmbient(isAmbient: Boolean) {
        ambient.value = isAmbient
    }

    fun onAmbientTick() = ambientTicks.update { it + 1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    val frames: Flow<Long> = ambient.flatMapLatest { isAmbient ->
        if (isAmbient) ambientTicks.map { ticker.now() } else ticker.frames()
    }
}
