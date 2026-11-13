package com.codebyramiz.breathefocus.core.data.time

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * Emits today's date now and again at each local midnight, so a screen that stays open
 * across midnight moves its "today" and streak figures over without a data change.
 */
fun Clock.today(): Flow<LocalDate> = flow {
    while (true) {
        val now = ZonedDateTime.now(this@today)
        emit(now.toLocalDate())
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1))
    }
}.distinctUntilChanged()
