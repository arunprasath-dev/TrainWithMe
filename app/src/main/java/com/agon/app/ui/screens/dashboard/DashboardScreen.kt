package com.agon.app.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agon.app.R
import com.agon.app.domain.model.plan.PlanDay
import com.agon.app.domain.model.plan.TrainingPlan
import com.agon.app.domain.model.profile.NutritionTargets
import com.agon.app.domain.model.profile.Units
import com.agon.app.domain.model.training.SessionStatus
import com.agon.app.domain.model.util.UnitConverter
import com.agon.app.ui.components.BannerTone
import com.agon.app.ui.components.EmberButton
import com.agon.app.ui.components.MessageBanner
import com.agon.app.ui.components.MuscleChip
import com.agon.app.ui.components.ProgressRing
import com.agon.app.ui.components.Reveal
import com.agon.app.ui.components.SectionTitle
import com.agon.app.ui.components.StatTile
import com.agon.app.ui.components.TwmCard
import com.agon.app.ui.theme.Ember
import com.agon.app.ui.theme.TrainWithMeColors
import com.agon.app.ui.theme.accent
import com.agon.app.viewmodel.AppViewModelFactory
import com.agon.app.viewmodel.DashboardUiState
import com.agon.app.viewmodel.DashboardViewModel
import com.agon.app.viewmodel.TodayKind
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardRoute(
    factory: AppViewModelFactory,
    onStartWorkout: (dayId: String) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenDay: (dayId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        state = state,
        onStartWorkout = onStartWorkout,
        onOpenPlan = onOpenPlan,
        onOpenSettings = onOpenSettings,
        onOpenOnboarding = onOpenOnboarding,
        onOpenDay = onOpenDay,
        onLogWeight = viewModel::logWeight,
        onRebuildPlan = viewModel::rebuildPlan,
        onDismissMessage = viewModel::dismissMessage,
        onRefresh = viewModel::refresh,
        modifier = modifier,
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
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nutritionExpanded by remember { mutableStateOf(false) }
    var showWeighIn by remember { mutableStateOf(false) }
    var dismissedReturnNote by remember(state.daysSinceLastCompletedSession) { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            DashboardTopBar(
                name = state.profile?.name?.takeIf { it.isNotBlank() },
                streak = state.streak,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(Modifier.fillMaxSize().padding(padding)) {
            AmbientBackground()
            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    state.message?.let { message ->
                        item(key = "msg") {
                            MessageBanner(
                                text = message,
                                tone = BannerTone.SUCCESS,
                                actionLabel = "Dismiss",
                                onAction = onDismissMessage,
                            )
                        }
                    }

                    if (state.todayKind == TodayKind.NO_PLAN || state.plan == null) {
                        item(key = "noplan") {
                            Reveal(0) { NoPlanHero(onOpenOnboarding) }
                        }
                        return@LazyColumn
                    }

                    val daysAway = state.daysSinceLastCompletedSession
                    if (daysAway != null && daysAway >= 14 && !dismissedReturnNote) {
                        item(key = "return") {
                            MessageBanner(
                                text = "Welcome back. Your next planned session is ready whenever you are — ease in at your own pace.",
                                tone = BannerTone.INFO,
                                actionLabel = "Dismiss",
                                onAction = { dismissedReturnNote = true },
                            )
                        }
                    }

                    if (state.needsPlanRebuild) {
                        item(key = "rebuild") {
                            MessageBanner(
                                text = "You updated your body data. Rebuild the plan so the prescriptions match your new numbers.",
                                tone = BannerTone.INFO,
                                actionLabel = if (state.isRebuilding) "Working…" else "Rebuild",
                                onAction = { if (!state.isRebuilding) onRebuildPlan() },
                            )
                        }
                    }

                    val plan = state.plan
                    val isCompleted = state.todaySession?.status == SessionStatus.COMPLETED
                    val resumeActive = state.activeSession != null &&
                        state.today?.id == state.activeSession.dayId &&
                        !isCompleted

                    item(key = "hero") {
                        Reveal(0) {
                            TodayHero(
                                day = state.today,
                                isCompleted = isCompleted,
                                isRestDay = state.todayKind != TodayKind.TRAINING,
                                resumeActive = resumeActive,
                                weekIndex = state.weekIndex,
                                weekLabel = plan.weeks.getOrNull(state.weekIndex)?.label.orEmpty(),
                                onStart = { state.today?.let { onStartWorkout(it.id) } },
                                onViewPlan = onOpenPlan,
                                onOpenDay = { state.today?.let { onOpenDay(it.id) } },
                            )
                        }
                    }

                    item(key = "week") {
                        Reveal(1) {
                            TwmCard {
                                SectionTitle(
                                    text = "This week",
                                    trailing = {
                                        TextButton(onClick = onOpenPlan) {
                                            Text("Full plan")
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                )
                                Spacer(Modifier.height(10.dp))
                                WeekStrip(
                                    days = state.weekDays,
                                    today = state.todayDate,
                                    completedDayIds = state.completedDayIds,
                                    onDayClick = { day -> onOpenDay(day.id) },
                                )
                            }
                        }
                    }

                    item(key = "adherence") {
                        Reveal(2) {
                            TwmCard {
                                SectionTitle("Momentum")
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProgressRing(
                                        progress = state.adherence,
                                        label = "${(state.adherence * 100).toInt()}%",
                                        caption = "this week",
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatTile(
                                            label = "Day streak",
                                            value = "${state.streak}",
                                            caption = if (state.streak == 1) "session in a row" else "sessions in a row",
                                        )
                                        StatTile(
                                            label = "Sessions",
                                            value = "${state.completedThisWeek}/${state.plannedThisWeek}",
                                            caption = "done this week",
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
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
                    }

                    item(key = "block") {
                        Reveal(3) {
                            BlockProgressCard(
                                plan = plan,
                                weekIndex = state.weekIndex,
                                completed = state.completedTotal,
                                onOpenPlan = onOpenPlan,
                            )
                        }
                    }

                    item(key = "nutrition") {
                        Reveal(4) {
                            NutritionCard(
                                nutrition = plan.nutrition,
                                expanded = nutritionExpanded,
                                onToggleExplanation = { nutritionExpanded = !nutritionExpanded },
                            )
                        }
                    }

                    item(key = "weight") {
                        Reveal(5) {
                            WeightCard(
                                state = state,
                                showWeighIn = showWeighIn,
                                onToggleWeighIn = { showWeighIn = !showWeighIn },
                                onSave = { weight ->
                                    onLogWeight(weight)
                                    showWeighIn = false
                                },
                            )
                        }
                    }

                    item(key = "footer") {
                        Text(
                            "Reminders and units live in settings.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Top bar + ambient background
// ---------------------------------------------------------------------------------------------

@Composable
private fun DashboardTopBar(
    name: String?,
    streak: Int,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar with ember ring
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(TrainWithMeColors.emberGradient, CircleShape)
                .padding(2.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name?.firstOrNull()?.uppercase() ?: "T",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = greetingLine(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = name?.takeIf { it.isNotBlank() } ?: "TrainWithMe",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (streak >= 2) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$streak",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}

private fun greetingLine(): String {
    val hour = runCatching { LocalTime.now().hour }.getOrDefault(10)
    return when (hour) {
        in 5..11 -> "GOOD MORNING · TIME TO TRAIN"
        in 12..16 -> "GOOD AFTERNOON · TIME TO TRAIN"
        in 17..21 -> "GOOD EVENING · TIME TO TRAIN"
        else -> "LATE SESSION · TIME TO TRAIN"
    }
}

/** Slow-drifting ember orbs behind the dashboard content — the screen breathes. */
@Composable
private fun AmbientBackground() {
    val transition = rememberInfiniteTransition(label = "ambient")
    val drift1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift1",
    )
    val drift2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift2",
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset(x = (-60 + drift1 * 50).dp, y = (-40 + drift1 * 70).dp)
                .size(260.dp)
                .blur(90.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(Ember.copy(alpha = 0.14f), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (40 - drift2 * 60).dp, y = (60 - drift2 * 80).dp)
                .size(300.dp)
                .blur(100.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f), CircleShape),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Today hero
// ---------------------------------------------------------------------------------------------

@Composable
private fun TodayHero(
    day: PlanDay?,
    isCompleted: Boolean,
    isRestDay: Boolean,
    resumeActive: Boolean,
    weekIndex: Int,
    weekLabel: String,
    onStart: () -> Unit,
    onViewPlan: () -> Unit,
    onOpenDay: () -> Unit,
) {
    val imageRes = if (isRestDay) R.drawable.rest_day else R.drawable.hero_gym
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { if (!isRestDay) onOpenDay() },
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .align(Alignment.BottomStart),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (isRestDay) Icons.Default.Hotel else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isRestDay) "RECOVERY DAY" else "WEEK ${weekIndex + 1} · ${weekLabel.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
                if (isCompleted && !isRestDay) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = Color(0xFF34D399), shape = RoundedCornerShape(10.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("DONE", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = day?.title ?: "No session today",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            if (!isRestDay && day != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${day.exerciseCount} exercises · ${day.totalSets} sets · about ${day.estimatedMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    day.focus.distinct().take(3).forEach { muscle ->
                        MuscleChip(label = muscle.label, color = Color.White)
                    }
                }
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = day?.activeRecovery ?: "Recovery is part of the programme. Stretch, walk, sleep.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isRestDay && day != null) {
                    EmberButton(
                        text = when {
                            resumeActive -> "Resume session"
                            isCompleted -> "Train again"
                            else -> "Start workout"
                        },
                        onClick = onStart,
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.weight(1.4f),
                    )
                }
                Surface(
                    onClick = onViewPlan,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open plan", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoPlanHero(onOpenOnboarding: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.onboarding_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.BottomStart),
        ) {
            Text(
                "YOUR COACH IS READY",
                style = MaterialTheme.typography.labelSmall,
                color = Ember,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Build your 8-week programme",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Answer a few questions about your body, goal and schedule — guided sessions, progression and nutrition included.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(16.dp))
            EmberButton(
                text = "Build my plan",
                onClick = onOpenOnboarding,
                icon = Icons.Default.FitnessCenter,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Week strip
// ---------------------------------------------------------------------------------------------

@Composable
private fun WeekStrip(
    days: List<PlanDay>,
    today: LocalDate,
    completedDayIds: Set<String>,
    onDayClick: (PlanDay) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        days.forEach { day ->
            val isToday = day.date == today
            val isDone = day.id in completedDayIds
            val isRest = day.isRestDay
            val container = when {
                isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                isToday && !isRest -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val content = when {
                isToday && !isRest && !isDone -> MaterialTheme.colorScheme.onPrimary
                isDone -> MaterialTheme.colorScheme.primary
                isRest -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            }
            Surface(
                onClick = { if (!isRest) onDayClick(day) },
                enabled = !isRest,
                color = container,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = content,
                    )
                    Spacer(Modifier.height(4.dp))
                    when {
                        isDone -> Icon(Icons.Default.Check, contentDescription = "Completed", tint = content, modifier = Modifier.size(17.dp))
                        isRest -> Icon(Icons.Default.Hotel, contentDescription = "Rest day", tint = content, modifier = Modifier.size(17.dp))
                        else -> Text(
                            text = day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
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

// ---------------------------------------------------------------------------------------------
// Block progress
// ---------------------------------------------------------------------------------------------

@Composable
private fun BlockProgressCard(
    plan: TrainingPlan,
    weekIndex: Int,
    completed: Int,
    onOpenPlan: () -> Unit,
) {
    val fraction = if (plan.totalPlannedSessions == 0) 0f
    else completed.toFloat() / plan.totalPlannedSessions
    TwmCard(onClick = onOpenPlan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "BLOCK PROGRESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(plan.splitName, style = MaterialTheme.typography.titleLarge)
                Text(
                    "$completed of ${plan.totalPlannedSessions} sessions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Week ${weekIndex + 1} of ${plan.weekCount} · ${plan.weeks[weekIndex].label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (plan.weeks[weekIndex].isDeload) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = TrainWithMeColors.success.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    "Deload week — lighter loads, stop 3–4 reps short of failure.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Nutrition
// ---------------------------------------------------------------------------------------------

@Composable
private fun NutritionCard(
    nutrition: NutritionTargets,
    expanded: Boolean,
    onToggleExplanation: () -> Unit,
) {
    TwmCard {
        SectionTitle(
            text = "Daily targets",
            trailing = {
                TextButton(onClick = onToggleExplanation) {
                    Text(if (expanded) "Hide maths" else "Why?")
                }
            },
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${nutrition.calories}",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "kcal\nper day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(14.dp))
        MacroBar(label = "Protein", grams = nutrition.proteinG, max = 250, color = Ember)
        Spacer(Modifier.height(8.dp))
        MacroBar(label = "Carbs", grams = nutrition.carbsG, max = 400, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(8.dp))
        MacroBar(label = "Fat", grams = nutrition.fatG, max = 150, color = TrainWithMeColors.success)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit = androidx.compose.animation.fadeOut(tween(100)) + shrinkVertically(tween(160)),
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
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
                com.agon.app.ui.components.DetailRow("Fibre", "${nutrition.fiberG} g")
                com.agon.app.ui.components.DetailRow("Water", "${nutrition.waterMl} ml")
                com.agon.app.ui.components.DetailRow("BMR / TDEE", "${nutrition.bmrKcal} / ${nutrition.tdeeKcal} kcal")
            }
        }
    }
}

@Composable
private fun MacroBar(label: String, grams: Int, max: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$grams g", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (grams.toFloat() / max).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Weight
// ---------------------------------------------------------------------------------------------

@Composable
private fun WeightCard(
    state: DashboardUiState,
    showWeighIn: Boolean,
    onToggleWeighIn: () -> Unit,
    onSave: (Double) -> Unit,
) {
    TwmCard {
        SectionTitle(
            text = "Body weight",
            trailing = {
                TextButton(onClick = onToggleWeighIn) {
                    Text(if (showWeighIn) "Close" else "Log")
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        val latest = state.latestMeasurement
        if (latest == null) {
            Text(
                "No weigh-ins yet. Log one weekly — the first two weeks build your trend.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                ) {
                    Icon(
                        Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                StatTile(
                    label = "Latest weigh-in",
                    value = UnitConverter.formatWeight(latest.weightKg, state.units),
                )
                Spacer(Modifier.weight(1f))
                state.weightDeltaKg?.let { delta ->
                    DeltaChip(deltaKg = delta, units = state.units)
                }
            }
        }
        AnimatedVisibility(
            visible = showWeighIn,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                QuickWeighIn(
                    suggestedKg = latest?.weightKg ?: state.profile?.weightKg ?: 70.0,
                    units = state.units,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun DeltaChip(deltaKg: Double, units: Units) {
    val icon = when {
        deltaKg > 0.05 -> Icons.Default.TrendingUp
        deltaKg < -0.05 -> Icons.Default.TrendingDown
        else -> Icons.Default.TrendingFlat
    }
    val tint = when {
        deltaKg > 0.05 -> TrainWithMeColors.warning
        deltaKg < -0.05 -> TrainWithMeColors.success
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val display = UnitConverter.kgToDisplay(kotlin.math.abs(deltaKg), units)
    val sign = if (deltaKg > 0.05) "+" else if (deltaKg < -0.05) "–" else ""
    Surface(color = tint.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "$sign${trim(display)} ${UnitConverter.unitLabel(units)}",
                style = MaterialTheme.typography.titleSmall,
                color = tint,
            )
        }
    }
}

@Composable
private fun QuickWeighIn(
    suggestedKg: Double,
    units: Units,
    onSave: (Double) -> Unit,
) {
    val suggestedDisplay = remember(suggestedKg, units) {
        trim(UnitConverter.kgToDisplay(suggestedKg, units))
    }
    var value by remember(suggestedDisplay) { mutableStateOf(suggestedDisplay) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { input -> value = input.filter { it.isDigit() || it == '.' }.take(6) },
            label = { Text("Weight") },
            suffix = { Text(UnitConverter.unitLabel(units)) },
            singleLine = true,
            isError = value.toDoubleOrNull() == null,
            supportingText = {
                if (value.toDoubleOrNull() == null) Text("Enter your weight")
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        EmberButton(
            text = "Save",
            onClick = {
                value.toDoubleOrNull()?.let {
                    onSave(UnitConverter.displayToKg(it, units))
                }
            },
            enabled = (value.toDoubleOrNull() ?: 0.0) > 0,
        )
    }
}

private fun trim(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
