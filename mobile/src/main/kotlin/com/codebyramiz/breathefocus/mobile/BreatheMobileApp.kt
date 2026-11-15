package com.codebyramiz.breathefocus.mobile

import android.app.Application
import com.codebyramiz.breathefocus.core.data.di.ApplicationScope
import com.codebyramiz.breathefocus.mobile.sync.SessionReconciler
import com.codebyramiz.breathefocus.mobile.sync.SettingsPublisher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class BreatheMobileApp : Application() {

    @Inject lateinit var settingsPublisher: SettingsPublisher

    @Inject lateinit var sessionReconciler: SessionReconciler

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // A choice that never reached the watch is sent again, and session chunks whose change
        // events were missed are read back from the Data Layer.
        scope.launch { settingsPublisher.publishPending() }
        scope.launch { sessionReconciler.reconcile() }
    }
}
