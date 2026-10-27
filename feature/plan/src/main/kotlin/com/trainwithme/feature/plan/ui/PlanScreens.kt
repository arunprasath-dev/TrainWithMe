package com.trainwithme.feature.plan.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainwithme.core.designsystem.component.EmptyState
import com.trainwithme.core.designsystem.component.PrimaryButton
import com.trainwithme.core.designsystem.component.SectionTitle
import com.trainwithme.core.designsystem.component.TwmCard
import com.trainwithme.core.model.plan.PlanDay
import com.trainwithme.feature.plan.component.DayCard
import com.trainwithme.core.designsystem.component.ExerciseRow
import com.trainwithme.feature.plan.component.PlanHeaderCard
import com.trainwithme.feature.plan.vm.DayDetailViewModel
import com.trainwithme.feature.plan.vm.PlanBrowserViewModel

// ---------------------------------------------------------------------------------------------
// Plan browser
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanBrowserRoute(
    onBack: () -> Unit,
    onOpenDay: (dayId: String) -> Unit,
    viewModel: PlanBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your programme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { Spacer(Modifier.navigationBarsPadding()) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.plan == null || state.week == null -> EmptyState(
                icon = Icons.Default.PlayArrow,
                title = "No plan yet",
                message = "Complete onboarding to generate your programme.",
                modifier = Modifier.padding(padding),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.weekTabs.forEach { index ->
                        val week = state.plan!!.weeks[index]
                        FilterChip(
                            selected = index == state.selectedWeekIndex,
                            onClick = { viewModel.selectWeek(index) },
                            label = { Text("W${index + 1}") },
                            trailingIcon = if (week.isDeload) {
                                { Text("–", style = MaterialTheme.typography.labelSmall) }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        val plan = state.plan!!
                        PlanHeaderCard(
                            plan = plan,
                            weekIndex = state.selectedWeekIndex,
                            completed = state.completedDayIds.size,
                        )
                    }
                    items(state.week!!.days, key = { it.id }) { day ->
                        DayCard(
                            day = day,
                            isToday = day.date == java.time.LocalDate.now(),
                            isCompleted = day.id in state.completedDayIds,
                            onClick = { if (!day.isRestDay) onOpenDay(day.id) },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Day detail
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailRoute(
    onBack: () -> Unit,
    onStart: (dayId: String) -> Unit,
    onOpenExercise: (exerciseId: String) -> Unit = {},
    viewModel: DayDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            state.weekLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (state.day != null && !state.day!!.isRestDay) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.background) {
                    PrimaryButton(
                        text = if (state.isCompleted) "Train again" else "Start this session",
                        onClick = { onStart(state.day!!.id) },
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .navigationBarsPadding(),
                    )
                }
            }
        },
    ) { padding ->
        val day = state.day
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (day == null) {
            EmptyState(
                icon = Icons.Default.PlayArrow,
                title = "Day not found",
                message = "This day is not part of your current plan.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        DayDetailContent(day = day, modifier = Modifier.padding(padding), onOpenExercise = onOpenExercise)
    }
}

@Composable
fun DayDetailContent(
    day: PlanDay,
    modifier: Modifier = Modifier,
    onOpenExercise: (String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (day.isRestDay) {
            TwmCard {
                SectionTitle("Rest & recover")
                Spacer(Modifier.height(6.dp))
                Text(
                    day.activeRecovery.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        TwmCard {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Summary("Exercises", "${day.exerciseCount}")
                Summary("Sets", "${day.totalSets}")
                Summary("Est. time", "${day.estimatedMinutes} min")
            }
        }

        SectionTitle("Warm-up")
        TwmCard {
            day.warmup.forEach { step ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("•  ", color = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(step.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            step.instructions,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        SectionTitle("Workout")
        day.exercises.forEachIndexed { index, exercise ->
            ExerciseRow(
                index = index,
                exercise = exercise,
                thumbnailAsset = "thumbnails/${exercise.exerciseId}.jpg",
                onClick = { onOpenExercise(exercise.exerciseId) },
            )
        }

        SectionTitle("Cool-down")
        TwmCard {
            day.cooldown.forEach { stretch ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("•  ", color = MaterialTheme.colorScheme.secondary)
                    Column {
                        Text("${stretch.name} · ${stretch.durationSeconds}s", style = MaterialTheme.typography.titleMedium)
                        Text(
                            stretch.instructions,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Summary(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
