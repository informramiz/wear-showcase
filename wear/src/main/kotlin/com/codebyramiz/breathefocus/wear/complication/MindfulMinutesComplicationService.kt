package com.codebyramiz.breathefocus.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.model.MindfulStats
import com.codebyramiz.breathefocus.wear.MainActivity
import com.codebyramiz.breathefocus.wear.R
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** "Today's mindful minutes" for any watch face, as short text or a ranged ring toward the goal. */
@AndroidEntryPoint
class MindfulMinutesComplicationService : SuspendingTimelineComplicationDataSourceService() {

    @Inject lateinit var repository: SessionRepository

    @Inject lateinit var clock: Clock

    /**
     * Today's total now, plus a zero-minute entry that takes over at the next local midnight,
     * so the face never labels yesterday's minutes as today's while waiting for a refresh.
     */
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val now = ZonedDateTime.now(clock)
        val today = now.toLocalDate()
        val startOfDay = today.atStartOfDay(now.zone).toInstant().toEpochMilli()
        val minutes = MindfulStats(clock.zone).minutesOn(today, repository.observeSince(startOfDay).first())
        val current = build(request.complicationType, minutes) ?: return null
        val midnight = today.plusDays(1).atStartOfDay(now.zone).toInstant()
        // Zero is the fallback outside any entry, so a lapsed refresh can never show an old day's total.
        val zero = build(request.complicationType, 0) ?: return ComplicationDataTimeline(current, emptyList())
        return ComplicationDataTimeline(
            zero,
            listOf(TimelineEntry(TimeInterval(Instant.ofEpochMilli(startOfDay), midnight), current)),
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, PREVIEW_MINUTES)

    private fun build(type: ComplicationType, minutes: Int): ComplicationData? {
        val text = PlainComplicationText.Builder("${minutes}m").build()
        val description = PlainComplicationText.Builder("$minutes mindful minutes today").build()
        val image = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_breathe)).build()
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text, description)
                .setMonochromaticImage(image)
                .setTapAction(tap)
                .build()
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = minutes.coerceAtMost(MindfulStats.DAILY_GOAL_MINUTES).toFloat(),
                min = 0f,
                max = MindfulStats.DAILY_GOAL_MINUTES.toFloat(),
                contentDescription = description,
            )
                .setText(text)
                .setMonochromaticImage(image)
                .setTapAction(tap)
                .build()
            else -> null
        }
    }

    private companion object {
        const val PREVIEW_MINUTES = 7
    }
}
