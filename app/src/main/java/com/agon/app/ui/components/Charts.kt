package com.agon.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agon.app.domain.model.progress.WeekVolume
import com.agon.app.domain.model.progress.WeightPoint
import com.agon.app.ui.theme.Ember
import com.agon.app.ui.theme.TrainWithMeColors
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Animated weight-trend chart: gradient area fill, glowing end-dot, min/max labels.
 * Draws a rolling-average smoothing for noisy body weight.
 */
@Composable
fun WeightTrendChart(
    points: List<WeightPoint>,
    modifier: Modifier = Modifier,
    unitLabel: String = "kg",
    accent: Color = Ember,
) {
    val reveal by animateFloatAsState(
        targetValue = if (points.size > 1) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chart reveal",
    )
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM", Locale.US) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (points.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Log a few more weigh-ins to draw your trend.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
            return
        }
        val values = points.map { it.weightKg }
        val min = values.min()
        val max = values.max()
        val span = (max - min).coerceAtLeast(0.5)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .semantics {
                    contentDescription = "Body weight trend from ${points.first().date.format(dateFmt)} " +
                        "to ${points.last().date.format(dateFmt)}"
                },
        ) {
            val w = size.width
            val h = size.height - 28.dp.toPx()
            val padX = 8.dp.toPx()
            fun x(i: Int) = padX + (w - padX * 2) * i / (points.size - 1)
            fun y(v: Double) = h - ((v - min + span * 0.12) / (span * 1.24)).toFloat() * h

            // gridlines
            repeat(4) { row ->
                val gy = h * (row + 1) / 5
                drawLine(
                    color = Color.Gray.copy(alpha = 0.18f),
                    start = Offset(padX, gy),
                    end = Offset(w - padX, gy),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val clipW = w * reveal
            with(drawContext.canvas) {
                save()
                clipRect(left = 0f, top = 0f, right = clipW, bottom = size.height)
                val line = Path()
                points.forEachIndexed { i, p ->
                    if (i == 0) line.moveTo(x(i), y(p.weightKg)) else line.lineTo(x(i), y(p.weightKg))
                }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(x(points.lastIndex), h)
                    lineTo(x(0), h)
                    close()
                }
                drawPath(
                    fill,
                    brush = Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.32f), accent.copy(alpha = 0.02f)),
                    ),
                )
                drawPath(line, color = accent, style = Stroke(width = 3.dp.toPx()))
                // glowing end dot
                val last = Offset(x(points.lastIndex), y(points.last().weightKg))
                drawCircle(accent.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = last)
                drawCircle(accent, radius = 4.5.dp.toPx(), center = last)
                drawCircle(Color.White, radius = 2.dp.toPx(), center = last)
                restore()
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                points.first().date.format(dateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${format1(points.last().weightKg)} $unitLabel",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Text(
                points.last().date.format(dateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Animated weekly volume bars with per-bar labels. */
@Composable
fun VolumeBarChart(
    weeks: List<WeekVolume>,
    modifier: Modifier = Modifier,
    unitLabel: String = "kg",
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM", Locale.US) }
    if (weeks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Complete a session and your training volume will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }
    val shown = weeks.takeLast(8)
    val max = shown.maxOf { it.volumeKg }.coerceAtLeast(1.0)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Weekly training volume, ${shown.size} weeks" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            shown.forEachIndexed { index, week ->
                val target = (week.volumeKg / max).toFloat().coerceIn(0.04f, 1f)
                val animated by animateFloatAsState(
                    targetValue = target,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                    ),
                    label = "bar $index",
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (week.volumeKg >= 1000) {
                        Text(
                            "${(week.volumeKg / 1000).let { format1(it) }}t",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp * animated)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Ember,
                                        Ember.copy(alpha = 0.55f),
                                    ),
                                ),
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                shown.first().weekStart.format(dateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "total ${format1(shown.sumOf { it.volumeKg })} $unitLabel · ${shown.sumOf { it.workingSets }} sets",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Compact sparkline for dashboard cards. */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    accent: Color = TrainWithMeColors.chartLine,
) {
    if (values.size < 2) return
    val min = values.min()
    val max = values.max()
    val span = (max - min).coerceAtLeast(0.001)
    val reveal by animateFloatAsState(targetValue = 1f, label = "spark")
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun x(i: Int) = w * i / (values.size - 1)
        fun y(v: Double) = h - ((v - min) / span).toFloat() * h * 0.85f - h * 0.075f
        val path = Path()
        values.forEachIndexed { i, v ->
            if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
        }
        with(drawContext.canvas) {
            save()
            clipRect(0f, 0f, w * reveal, h)
            drawPath(path, accent, style = Stroke(2.5.dp.toPx()))
            drawCircle(accent, 3.dp.toPx(), Offset(x(values.lastIndex), y(values.last())))
            restore()
        }
    }
}

private fun format1(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
