package com.trainwithme.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trainwithme.core.designsystem.theme.TrainWithMeTheme
import com.trainwithme.core.model.progress.WeightPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Minimal line chart for body-composition trends. Deliberately dependency-free (no chart library):
 * one axis, no interaction, honest empty states. Values are labelled at the ends so the user can
 * read the actual number rather than estimating from pixels.
 */
@Composable
fun TrendLineChart(
    points: List<WeightPoint>,
    modifier: Modifier = Modifier,
    unitLabel: String = "kg",
    seriesLabel: String = "Weight",
    emptyMessage: String = "Log two weigh-ins to see your trend.",
) {
    if (points.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val sorted = points.sortedBy { it.date }
    val min = sorted.minOf { it.weightKg }
    val max = sorted.maxOf { it.weightKg }
    val span = (max - min).takeIf { it > 0.05 } ?: 1.0

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .semantics {
                    val delta = sorted.last().weightKg - sorted.first().weightKg
                    val direction = when {
                        delta > 0.05 -> "up"
                        delta < -0.05 -> "down"
                        else -> "about the same"
                    }
                    contentDescription = "$seriesLabel trend, ${sorted.size} measurements. " +
                        "From ${formatNumber(sorted.first().weightKg)} $unitLabel on ${formatDate(sorted.first().date)} " +
                        "to ${formatNumber(sorted.last().weightKg)} $unitLabel on ${formatDate(sorted.last().date)}; " +
                        "overall $direction by ${formatNumber(kotlin.math.abs(delta))} $unitLabel."
                }
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            val w = size.width
            val h = size.height
            // horizontal grid
            repeat(4) { index ->
                val y = h * index / 3f
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            val stepX = w / (sorted.size - 1)
            fun yOf(value: Double): Float = (h - ((value - min) / span * h * 0.9f + h * 0.05f)).toFloat()

            val line = Path()
            sorted.forEachIndexed { index, point ->
                val x = stepX * index
                val y = yOf(point.weightKg)
                if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
            }
            // soft fill under the line
            val fill = Path().apply {
                addPath(line)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                fill,
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                ),
            )
            drawPath(line, color = lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
            sorted.forEachIndexed { index, point ->
                drawCircle(
                    color = lineColor,
                    radius = if (index == sorted.lastIndex) 6f else 3.5f,
                    center = Offset(stepX * index, yOf(point.weightKg)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDate(sorted.first().date),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
            Text(
                text = "${formatNumber(sorted.last().weightKg)} $unitLabel",
                style = MaterialTheme.typography.labelLarge,
                color = lineColor,
            )
            Text(
                text = formatDate(sorted.last().date),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
        }
    }
}

/** Simple bar chart for weekly training volume or adherence. */
@Composable
fun WeeklyBarChart(
    values: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No completed sessions yet.",
    valueFormatter: (Double) -> String = { it.roundToInt().toString() },
) {
    if (values.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val barColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val max = values.maxOf { it.second }.takeIf { it > 0 } ?: 1.0
    val lastIndex = values.lastIndex

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .semantics {
                    contentDescription = "Weekly training volume. " + values.joinToString(separator = "; ") { (label, value) ->
                        "$label: ${valueFormatter(value)}"
                    }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            val gap = size.width / (values.size * 4f)
            val barWidth = (size.width - gap * (values.size + 1)) / values.size
            values.forEachIndexed { index, (_, value) ->
                val height = (value / max * size.height).toFloat().coerceAtLeast(3f)
                val left = gap + index * (barWidth + gap)
                drawRoundRect(
                    color = if (index == lastIndex) barColor else mutedColor,
                    topLeft = Offset(left, size.height - height),
                    size = androidx.compose.ui.geometry.Size(barWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3f, barWidth / 3f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            values.forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        valueFormatter(value),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Circular progress used for weekly adherence. */
@Composable
fun ProgressRing(
    progress: Float,
    label: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 650),
        label = "adherence progress",
    )
    val track = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.size(112.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(112.dp)) {
            val stroke = 10.dp.toPx()
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleLarge)
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMM"))

private fun formatNumber(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

@Preview(showBackground = true)
@Composable
private fun ChartsPreview() {
    TrainWithMeTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TrendLineChart(
                points = listOf(
                    WeightPoint(LocalDate.of(2026, 9, 1), 82.4),
                    WeightPoint(LocalDate.of(2026, 9, 8), 81.9),
                    WeightPoint(LocalDate.of(2026, 9, 15), 81.6),
                    WeightPoint(LocalDate.of(2026, 9, 22), 81.1),
                ),
            )
            WeeklyBarChart(values = listOf("W1" to 12.0, "W2" to 14.0, "W3" to 11.0, "W4" to 15.0))
            ProgressRing(progress = 0.75f, label = "75%", caption = "adherence")
        }
    }
}
