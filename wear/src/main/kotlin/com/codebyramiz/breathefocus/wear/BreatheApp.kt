package com.codebyramiz.breathefocus.wear

import android.app.Application
import com.codebyramiz.breathefocus.wear.di.ApplicationScope
import com.codebyramiz.breathefocus.wear.session.SessionPublisher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class BreatheApp : Application() {

    @Inject lateinit var publisher: SessionPublisher

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // A push that failed while the phone was away is retried on the next start.
        scope.launch { publisher.retryIfPending() }
    }
}
