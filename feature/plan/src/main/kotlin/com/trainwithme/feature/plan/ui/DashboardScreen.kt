package com.trainwithme.feature.plan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainwithme.core.designsystem.component.BannerTone
import com.trainwithme.core.designsystem.component.MessageBanner
import com.trainwithme.core.designsystem.component.PrimaryButton
import com.trainwithme.core.designsystem.component.ProgressRing
import com.trainwithme.core.designsystem.component.SectionTitle
import com.trainwithme.core.designsystem.component.StatTile
import com.trainwithme.core.designsystem.component.TwmCard
import com.trainwithme.core.designsystem.theme.TrainWithMeTheme
import com.trainwithme.core.model.training.SessionStatus
import com.trainwithme.feature.plan.component.NutritionCard
import com.trainwithme.feature.plan.component.PlanHeaderCard
import com.trainwithme.feature.plan.component.TodayCard
import com.trainwithme.feature.plan.component.WeekStrip
import com.trainwithme.feature.plan.vm.DashboardUiState
import com.trainwithme.feature.plan.vm.DashboardViewModel
import com.trainwithme.feature.plan.vm.TodayKind

/**
 * Home screen. Receives navigation callbacks from :app and nothing else — all data comes from the
 * ViewModel's single immutable state.
 */
@Composable
fun DashboardRoute(
    onStartWorkout: (dayId: String) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenDay: (dayId: String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        state = state,
        onStartWorkout = { dayId -> onStartWorkout(dayId) },
        onOpenPlan = onOpenPlan,
        onOpenSettings = onOpenSettings,
        onOpenOnboarding = onOpenOnboarding,
        onOpenDay = onOpenDay,
        onLogWeight = viewModel::logWeight,
        onRebuildPlan = viewModel::rebuildPlan,
        onDismissMessage = viewModel::dismissMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onStartWorkout: (String) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenDay: (String) -> Unit,
    onLogWeight: (Double) -> Unit,
    onRebuildPlan: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nutritionExpanded by remember { mutableStateOf(false) }
    var showWeighIn by remember { mutableStateOf(false) }
    var dismissedReturnNote by remember(state.daysSinceLastCompletedSession) { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.055f),
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.background,
                ),
            ),
        ),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = greeting(state),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = state.profile?.name?.takeIf { it.isNotBlank() } ?: "TrainWithMe",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { Spacer(Modifier.navigationBarsPadding()) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.message?.let { message ->
                MessageBanner(
                    text = message,
                    tone = BannerTone.SUCCESS,
                    actionLabel = "Dismiss",
                    onAction = onDismissMessage,
                )
            }

            if (state.todayKind == TodayKind.NO_PLAN || state.plan == null) {
                NoPlanCard(onOpenOnboarding)
                return@Column
            }

            val daysAway = state.daysSinceLastCompletedSession
            if (daysAway != null && daysAway >= 14 && !dismissedReturnNote) {
                MessageBanner(
                    text = "Welcome back. Your next planned session is ready whenever you are — ease in at your own pace.",
                    tone = BannerTone.INFO,
                    actionLabel = "Dismiss",
                    onAction = { dismissedReturnNote = true },
                )
            }

            if (state.needsPlanRebuild) {
                MessageBanner(
                    text = "You updated your body data. Rebuild the plan so the prescriptions match your new numbers.",
                    tone = BannerTone.INFO,
                    actionLabel = if (state.isRebuilding) "Working…" else "Rebuild",
                    onAction = { if (!state.isRebuilding) onRebuildPlan() },
                )
            }

            val plan = state.plan

            DashboardReveal(index = 0) {
                TodayCard(
                    day = state.today,
                    isCompleted = state.todaySession?.status == SessionStatus.COMPLETED,
                    isRestDay = state.todayKind != TodayKind.TRAINING,
                    onStart = { state.today?.let { onStartWorkout(it.id) } },
                    onViewPlan = onOpenPlan,
                )
            }

            DashboardReveal(index = 1) {
                WeekStrip(
                    days = state.weekDays,
                    today = state.todayDate,
                    completedDayIds = state.completedDayIds,
                    onDayClick = { day -> onOpenDay(day.id) },
                )
            }

            DashboardReveal(index = 2) {
                TwmCard {
                    SectionTitle("This week")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(
                            progress = state.adherence,
                            label = "${(state.adherence * 100).toInt()}%",
                            caption = "done",
                        )
                        Spacer(Modifier.size(12.dp))
                        StatTile(
                            label = "Streak",
                            value = "${state.streak}",
                            caption = "sessions in a row",
                        )
                    }
                    val reflection = when {
                        state.completedThisWeek == 0 -> "A fresh week. Start when you're ready."
                        state.plannedThisWeek > 0 && state.completedThisWeek >= state.plannedThisWeek ->
                            "Your planned sessions are done. Recovery counts, too."
                        else -> "${state.completedThisWeek} session${if (state.completedThisWeek == 1) "" else "s"} done this week. Keep going at your pace."
                    }
                    Text(
                        reflection,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            DashboardReveal(index = 3) {
                TwmCard {
                    SectionTitle("Block progress")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${state.completedTotal} of ${state.plannedTotal} planned sessions completed",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    PlanHeaderCard(
                        plan = plan,
                        weekIndex = state.weekIndex,
                        completed = state.completedTotal,
                        showRationale = false,
                    )
                    TextButton(onClick = onOpenPlan) { Text("View all weeks") }
                }
            }

            DashboardReveal(index = 4) {
                NutritionCard(
                    nutrition = plan.nutrition,
                    expanded = nutritionExpanded,
                    onToggleExplanation = { nutritionExpanded = !nutritionExpanded },
                )
            }

            DashboardReveal(index = 5) {
                TwmCard {
                    SectionTitle(
                        text = "Body weight",
                        trailing = {
                            TextButton(onClick = { showWeighIn = !showWeighIn }) {
                                Text(if (showWeighIn) "Close" else "Log")
                            }
                        },
                    )
                    val latest = state.latestMeasurement
                    if (latest == null) {
                        Text(
                            "No weigh-ins yet. Log one weekly — the first two weeks build your trend.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatTile(
                                label = "Latest",
                                value = "${trim(latest.weightKg)} kg",
                                caption = state.weightDeltaKg?.let { delta ->
                                    val sign = if (delta > 0) "+" else ""
                                    "$sign${trim(delta)} kg vs previous"
                                } ?: "first entry",
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = showWeighIn,
                        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 10 },
                    ) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            QuickWeighIn(
                                suggestedKg = latest?.weightKg ?: state.profile?.weightKg ?: 70.0,
                                onSave = { weight ->
                                    onLogWeight(weight)
                                    showWeighIn = false
                                },
                            )
                        }
                    }
                }
            }

            Text(
                "Reminders and units live in settings.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DashboardReveal(index: Int, content: @Composable () -> Unit) {
    val visibility = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibility.targetState = true }
    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn(tween(durationMillis = 220, delayMillis = index * 30)) +
            slideInVertically(tween(durationMillis = 260, delayMillis = index * 30)) { it / 16 },
    ) {
        content()
    }
}

@Composable
private fun NoPlanCard(onOpenOnboarding: () -> Unit) {
    TwmCard {
        SectionTitle("No active plan yet")
        Spacer(Modifier.height(8.dp))
        Text(
            "Answer a few questions about your body, goal and schedule and we'll build an 8-week " +
                "programme with guided sessions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = "Build my plan",
            onClick = onOpenOnboarding,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QuickWeighIn(suggestedKg: Double, onSave: (Double) -> Unit) {
    var value by remember { mutableStateOf(trim(suggestedKg)) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { input -> value = input.filter { it.isDigit() || it == '.' }.take(5) },
            label = { Text("Weight") },
            suffix = { Text("kg") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
            ),
        )
        PrimaryButton(
            text = "Save",
            onClick = { value.toDoubleOrNull()?.let(onSave) },
            enabled = value.toDoubleOrNull() != null,
        )
    }
}

private fun greeting(state: DashboardUiState): String = when (state.todayKind) {
    TodayKind.TRAINING -> "YOUR SESSION TODAY"
    TodayKind.REST -> "RECOVERY DAY"
    TodayKind.NO_PLAN -> "WELCOME"
}

private fun trim(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun DashboardPreview() {
    TrainWithMeTheme(darkTheme = true) {
        DashboardContent(
            state = DashboardUiState(isLoading = false, todayKind = TodayKind.NO_PLAN),
            onStartWorkout = {},
            onOpenPlan = {},
            onOpenSettings = {},
            onOpenOnboarding = {},
            onOpenDay = {},
            onLogWeight = {},
            onRebuildPlan = {},
            onDismissMessage = {},
        )
    }
}
