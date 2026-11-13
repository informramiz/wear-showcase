package com.codebyramiz.breathefocus.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codebyramiz.breathefocus.core.model.DayTotal
import java.time.format.TextStyle

/** Seven-day bar chart drawn on a Canvas, with a dashed line at the daily goal. */
@Composable
fun WeeklyChart(week: List<DayTotal>, goalMinutes: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val goalColor = MaterialTheme.colorScheme.tertiary
    val locale = LocalLocale.current.platformLocale
    val grow = remember { Animatable(0f) }
    LaunchedEffect(week) { grow.animateTo(1f, tween(durationMillis = 700)) }
    val maxValue = maxOf(goalMinutes, week.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1) * 1.15f

    Column(modifier.testTag("weekly_chart")) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            if (week.isEmpty()) return@Canvas
            val slot = size.width / week.size
            val barWidth = slot * 0.5f
            val radius = CornerRadius(barWidth / 2, barWidth / 2)
            week.forEachIndexed { i, day ->
                val left = slot * i + (slot - barWidth) / 2
                drawRoundRect(track, Offset(left, 0f), Size(barWidth, size.height), radius)
                val h = size.height * (day.minutes / maxValue) * grow.value
                if (h > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(primary, secondary),
                            startY = size.height - h,
                            endY = size.height,
                        ),
                        topLeft = Offset(left, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = radius,
                    )
                }
            }
            val goalY = size.height * (1 - goalMinutes / maxValue)
            drawLine(
                color = goalColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            week.forEach { day ->
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
