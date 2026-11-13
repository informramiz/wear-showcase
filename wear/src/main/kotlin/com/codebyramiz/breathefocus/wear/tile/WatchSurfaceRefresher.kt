package com.codebyramiz.breathefocus.wear.tile

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.codebyramiz.breathefocus.wear.complication.MindfulMinutesComplicationService
import com.codebyramiz.breathefocus.wear.session.SurfaceRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WatchSurfaceRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) : SurfaceRefresher {
    override fun refresh() {
        TileService.getUpdater(context).requestUpdate(BreatheTileService::class.java)
        ComplicationDataSourceUpdateRequester
            .create(context, ComponentName(context, MindfulMinutesComplicationService::class.java))
            .requestUpdateAll()
    }
}
