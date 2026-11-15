package com.codebyramiz.breathefocus.wear.sync

import android.util.Log
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.di.ApplicationScope
import com.codebyramiz.breathefocus.core.data.sync.DataLayerReader
import com.codebyramiz.breathefocus.core.model.BreathingPattern
import com.codebyramiz.breathefocus.wear.session.SurfaceRefresher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Applies the default pattern chosen on the phone. A change event can be missed (the receiver
 * timed out, the app was not running), so [reconcile] reads the stored Data Layer item instead.
 * Importing the same value twice changes nothing.
 */
@Singleton
class DefaultPatternImporter @Inject constructor(
    private val settings: SettingsRepository,
    private val reader: DataLayerReader,
    private val refresher: SurfaceRefresher,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /** Saves [pattern] and refreshes the tile, which shows it, if it differs from what is stored. */
    suspend fun import(pattern: BreathingPattern) {
        if (settings.defaultPattern.first() == pattern) return
        settings.setDefaultPattern(pattern)
        refresher.refresh()
    }

    suspend fun reconcile() {
        try {
            reader.defaultPattern()?.let { import(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not reconcile the default pattern", e)
        }
    }

    /** Reconciles outside the caller, which may be about to be torn down. */
    fun reconcileInBackground(): Job = scope.launch { reconcile() }

    private companion object {
        const val TAG = "DefaultPatternImporter"
    }
}
