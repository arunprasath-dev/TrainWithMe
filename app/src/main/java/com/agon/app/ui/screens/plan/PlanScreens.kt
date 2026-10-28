package com.agon.app.ui.screens.plan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agon.app.R
import com.agon.app.domain.model.plan.PlanDay
import com.agon.app.ui.components.EmberButton
import com.agon.app.ui.components.EmptyState
import com.agon.app.ui.components.ExerciseArt
import com.agon.app.ui.components.ExerciseRow
import com.agon.app.ui.components.MuscleChip
import com.agon.app.ui.components.Reveal
import com.agon.app.ui.components.SectionTitle
import com.agon.app.ui.components.TwmCard
import com.agon.app.ui.theme.accent
import com.agon.app.viewmodel.AppViewModelFactory
import com.agon.app.viewmodel.DayDetailViewModel
import com.agon.app.viewmodel.PlanBrowserViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ---------------------------------------------------------------------------------------------
// Plan browser
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanBrowserRoute(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onOpenDay: (dayId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanBrowserViewModel = viewModel(factory = factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Your programme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.plan == null || state.week == null -> EmptyState(
                icon = Icons.Default.CalendarMonth,
                title = "No plan yet",
                message = "Complete onboarding to generate your programme.",
                modifier = Modifier.padding(padding),
            )
            else -> {
                val plan = state.plan!!
                val week = state.week!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "header") {
                        Reveal(0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(26.dp)),
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.plan_header),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(172.dp),
                                )
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Black.copy(alpha = 0.25f),
                                                    Color.Black.copy(alpha = 0.82f),
                                                ),
                                            ),
                                        ),
                                )
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp)
                                        .align(Alignment.BottomStart),
                                ) {
                                    Text(
                                        "WEEK ${state.selectedWeekIndex + 1} OF ${plan.weekCount} · ${week.label.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        plan.splitName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White,
                                    )
                                    Text(
                                        "${plan.totalPlannedSessions} sessions · ${state.completedDayIds.size} done",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }
                    item(key = "tabs") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.weekTabs.forEach { index ->
                                val w = plan.weeks[index]
                                FilterChip(
                                    selected = index == state.selectedWeekIndex,
                                    onClick = { viewModel.selectWeek(index) },
                                    label = { Text("W${index + 1}") },
                                    trailingIcon = if (w.isDeload) {
                                        { Text("· deload", style = MaterialTheme.typography.labelSmall) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                )
                            }
                        }
                    }
                    if (week.isDeload) {
                        item(key = "deload") {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    "Deload week — lighter loads, stop 3–4 reps short of failure.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                    item(key = "rationale") {
                        TwmCard {
                            SectionTitle("Why this plan")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                plan.rationale,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    item(key = "days-title") {
                        SectionTitle("Week ${state.selectedWeekIndex + 1} sessions")
                    }
                    items(week.days, key = { it.id }) { day ->
                        val today = runCatching { LocalDate.now() }.getOrNull()
                        DayCard(
                            day = day,
                            isToday = day.date == today,
                            isCompleted = day.id in state.completedDayIds,
                            onClick = { if (!day.isRestDay) onOpenDay(day.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    day: PlanDay,
    isToday: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
) {
    TwmCard(onClick = if (day.isRestDay) null else onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (day.isRestDay) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        Icons.Default.Hotel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp),
                    )
                }
            } else {
                val first = day.exercises.firstOrNull()
                ExerciseArt(
                    assetPath = "thumbnails/${first?.exerciseId}.jpg",
                    contentDescription = null,
                    size = 54.dp,
                    muscle = day.focus.firstOrNull(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US) + " · " + day.date.dayOfMonth,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (day.isRestDay) "Rest & recover" else day.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!day.isRestDay) {
                    Text(
                        text = "${day.exerciseCount} exercises · ${day.totalSets} sets · ~${day.estimatedMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = "Walk, stretch, sleep",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when {
                isCompleted -> StatusPill("Done", filled = true)
                isToday && !day.isRestDay -> StatusPill("Today", filled = true)
                day.isRestDay -> StatusPill("Rest", filled = false)
                else -> Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, filled: Boolean) {
    Surface(
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (text == "Done") {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Day detail
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailRoute(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayDetailViewModel = viewModel(factory = factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.day?.title ?: "Day", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (state.weekLabel.isNotBlank()) {
                            Text(
                                state.weekLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            if (state.day != null && !state.day!!.isRestDay) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .padding(bottom = 8.dp),
                ) {
                    EmberButton(
                        text = if (state.isCompleted) "Train again" else "Start workout",
                        onClick = onStart,
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val day = state.day
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            day == null -> EmptyState(
                icon = Icons.Default.CalendarMonth,
                title = "Day not found",
                message = "This day is not part of your active plan.",
                modifier = Modifier.padding(padding),
            )
            day.isRestDay -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Reveal(0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp)),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.rest_day),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                            )
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                        ),
                                    ),
                            )
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                                    .align(Alignment.BottomStart),
                            ) {
                                Text("REST & RECOVER", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                                Text("Recovery day", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            }
                        }
                    }
                }
                item {
                    Reveal(1) {
                        TwmCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                                    Icon(
                                        Icons.Default.Spa,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .size(22.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    day.activeRecovery ?: "Recovery is part of the programme. Stretch, walk, sleep.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "stats") {
                    Reveal(0) {
                        TwmCard {
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                DayStat("Exercises", "${day.exerciseCount}")
                                DayStat("Sets", "${day.totalSets}")
                                DayStat("Est. time", "${day.estimatedMinutes} min")
                            }
                            if (day.focusLabel.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    day.focus.distinct().take(4).forEach { muscle ->
                                        MuscleChip(
                                            label = muscle.label,
                                            color = muscle.accent(),
                                        )
                                    }
                                }
                            }
                            if (state.isCompleted) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Completed — nice work.", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                if (day.warmup.isNotEmpty()) {
                    item(key = "warmup-title") { SectionTitle("Warm-up") }
                    item(key = "warmup") {
                        Reveal(1) {
                            TwmCard {
                                day.warmup.forEach { step ->
                                    Column(Modifier.padding(vertical = 6.dp)) {
                                        Text(step.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            step.instructions,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "ex-title") { SectionTitle("The session") }
                items(day.exercises, key = { it.exerciseId + it.slotIndex }) { exercise ->
                    ExerciseRow(
                        index = exercise.slotIndex,
                        exercise = exercise,
                        thumbnailAsset = "thumbnails/${exercise.exerciseId}.jpg",
                    )
                }
                if (day.cooldown.isNotEmpty()) {
                    item(key = "cool-title") { SectionTitle("Cool-down") }
                    item(key = "cool") {
                        TwmCard {
                            day.cooldown.forEach { stretch ->
                                Text(
                                    "• ${stretch.name} · ${stretch.durationSeconds}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
                item(key = "spacer") { Spacer(Modifier.height(70.dp)) }
            }
        }
    }
}

@Composable
private fun DayStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
