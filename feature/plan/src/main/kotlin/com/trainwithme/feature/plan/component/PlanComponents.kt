package com.trainwithme.feature.plan.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trainwithme.core.designsystem.component.DetailRow
import com.trainwithme.core.designsystem.component.SectionTitle
import com.trainwithme.core.designsystem.component.TwmCard
import com.trainwithme.core.model.plan.PlanDay
import com.trainwithme.core.model.plan.TrainingPlan
import com.trainwithme.core.model.profile.NutritionTargets
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Monday-first week strip showing done / today / upcoming / rest for the current week. */
@Composable
fun WeekStrip(
    days: List<PlanDay>,
    today: LocalDate,
    completedDayIds: Set<String>,
    onDayClick: (PlanDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEach { day ->
            val isToday = day.date == today
            val isDone = day.id in completedDayIds
            val isRest = day.isRestDay
            val container = when {
                isRest -> MaterialTheme.colorScheme.surfaceContainer
                isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val content = when {
                isToday && !isRest -> MaterialTheme.colorScheme.onPrimary
                isDone -> MaterialTheme.colorScheme.primary
                isRest -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            }
            Surface(
                onClick = { if (!isRest) onDayClick(day) },
                enabled = !isRest,
                color = container,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).take(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = content,
                    )
                    Spacer(Modifier.height(4.dp))
                    when {
                        isDone -> Icon(Icons.Default.Check, contentDescription = "Completed", tint = content, modifier = Modifier.size(16.dp))
                        isRest -> Icon(Icons.Default.Hotel, contentDescription = "Rest day", tint = content, modifier = Modifier.size(16.dp))
                        else -> Text(
                            text = day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = content,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isRest) "rest" else "${day.exerciseCount} ex",
                        style = MaterialTheme.typography.labelSmall,
                        color = content.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/** Card summarising a plan day, used in the plan browser. */
@Composable
fun DayCard(
    day: PlanDay,
    isToday: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TwmCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US) + " · " + day.date.dayOfMonth,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (day.isRestDay) "Rest & recover" else day.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!day.isRestDay) {
                    Text(
                        text = "${day.exerciseCount} exercises · ${day.totalSets} sets · about ${day.estimatedMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when {
                isCompleted -> StatusPill("Done", MaterialTheme.colorScheme.primary)
                isToday -> StatusPill("Today", MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                day.isRestDay -> StatusPill("Rest", MaterialTheme.colorScheme.surfaceContainerHigh)
            }
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    val content = if (color == MaterialTheme.colorScheme.primary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color, shape = CircleShape) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** Nutrition targets with an expandable explanation ("show me the maths"). */
@Composable
fun NutritionCard(
    nutrition: NutritionTargets,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onToggleExplanation: () -> Unit = {},
) {
    TwmCard(modifier = modifier) {
        SectionTitle(
            text = "Daily targets",
            trailing = {
                androidx.compose.material3.TextButton(onClick = onToggleExplanation) {
                    Text(if (expanded) "Hide" else "Why?")
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MacroValue("${nutrition.calories}", "kcal", MaterialTheme.colorScheme.primary)
            MacroValue("${nutrition.proteinG} g", "protein", MaterialTheme.colorScheme.onSurface)
            MacroValue("${nutrition.fatG} g", "fat", MaterialTheme.colorScheme.onSurface)
            MacroValue("${nutrition.carbsG} g", "carbs", MaterialTheme.colorScheme.onSurface)
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) +
                androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(220)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100)) +
                androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(160)),
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                nutrition.explanation.forEach { line ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                DetailRow("Fibre", "${nutrition.fiberG} g")
                DetailRow("Water", "${nutrition.waterMl} ml")
                DetailRow("BMR / TDEE", "${nutrition.bmrKcal} / ${nutrition.tdeeKcal} kcal")
            }
        }
    }
}

@Composable
private fun MacroValue(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 8.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Header for a plan: split, block progress and the rationale behind the programme. */
@Composable
fun PlanHeaderCard(
    plan: TrainingPlan,
    weekIndex: Int,
    completed: Int,
    modifier: Modifier = Modifier,
    showRationale: Boolean = true,
) {
    TwmCard(modifier = modifier) {
        Text(
            "WEEK ${weekIndex + 1} OF ${plan.weekCount} · ${plan.weeks[weekIndex].label.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(plan.splitName, style = MaterialTheme.typography.headlineSmall)
        Text(
            "$completed of ${plan.totalPlannedSessions} sessions completed in this block",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (plan.weeks[weekIndex].isDeload) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    "Deload week — lighter loads, stop 3–4 reps short of failure.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        if (showRationale) {
            Spacer(Modifier.height(10.dp))
            Text(
                plan.rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Today's session card — the primary call to action on the dashboard. */
@Composable
fun TodayCard(
    day: PlanDay?,
    isCompleted: Boolean,
    isRestDay: Boolean,
    onStart: () -> Unit,
    onViewPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TwmCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = if (isRestDay) "REST DAY" else "TODAY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = day?.title ?: "No session today",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(6.dp))
        if (isRestDay || day == null) {
            Text(
                day?.activeRecovery ?: "Recovery is part of the programme. Stretch, walk, sleep.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "${day.focusLabel} · ${day.exerciseCount} exercises · about ${day.estimatedMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isRestDay && day != null) {
                Surface(
                    onClick = onStart,
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isCompleted) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = if (isCompleted) "Log another session" else "Start workout",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Surface(
                onClick = onViewPlan,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Open plan", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

