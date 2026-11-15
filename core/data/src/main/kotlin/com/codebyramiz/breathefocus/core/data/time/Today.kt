package com.codebyramiz.breathefocus.core.data.time

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transformLatest

/**
 * Emits today's date now and again at each local midnight, so a screen that stays open
 * across midnight moves its "today" and streak figures over without a data change. Every
 * event of [timeChanges] (the date, time or zone was changed) recomputes the wait for the
 * next midnight, so the date never lags behind a clock adjustment.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Clock.today(timeChanges: Flow<Unit>): Flow<LocalDate> = merge(flowOf(Unit), timeChanges)
    .transformLatest {
        while (true) {
            val now = ZonedDateTime.now(this@today)
            emit(now.toLocalDate() to now.zone)
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1))
        }
    }
    // A zone change can regroup sessions even when the date reads the same, so the zone is part of the key.
    .distinctUntilChanged()
    .map { it.first }
