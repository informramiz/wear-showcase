package com.codebyramiz.breathefocus.wear.di

import com.codebyramiz.breathefocus.wear.haptics.Haptics
import com.codebyramiz.breathefocus.wear.haptics.VibratorHaptics
import com.codebyramiz.breathefocus.wear.health.HealthServicesHeartRateMonitor
import com.codebyramiz.breathefocus.wear.health.HeartRateMonitor
import com.codebyramiz.breathefocus.wear.session.MonotonicSessionTicker
import com.codebyramiz.breathefocus.wear.session.OngoingSessionNotifier
import com.codebyramiz.breathefocus.wear.session.SessionNotifier
import com.codebyramiz.breathefocus.wear.session.SessionTicker
import com.codebyramiz.breathefocus.wear.session.SurfaceRefresher
import com.codebyramiz.breathefocus.wear.tile.WatchSurfaceRefresher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WearModule {
    @Binds abstract fun haptics(impl: VibratorHaptics): Haptics

    @Binds abstract fun heartRate(impl: HealthServicesHeartRateMonitor): HeartRateMonitor

    @Binds abstract fun notifier(impl: OngoingSessionNotifier): SessionNotifier

    @Binds abstract fun ticker(impl: MonotonicSessionTicker): SessionTicker

    @Binds abstract fun refresher(impl: WatchSurfaceRefresher): SurfaceRefresher
}
