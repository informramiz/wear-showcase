package com.codebyramiz.breathefocus.wear

import android.app.Application
import com.codebyramiz.breathefocus.core.data.di.ApplicationScope
import com.codebyramiz.breathefocus.wear.session.SessionPublisher
import com.codebyramiz.breathefocus.wear.sync.DefaultPatternImporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class BreatheApp : Application() {

    @Inject lateinit var publisher: SessionPublisher

    @Inject lateinit var patternImporter: DefaultPatternImporter

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            // A push that failed while the phone was away is retried on the next start.
            publisher.retryIfPending()
            // A setting change event missed while the app was not running is read back from the Data Layer.
            patternImporter.reconcile()
        }
    }
}
