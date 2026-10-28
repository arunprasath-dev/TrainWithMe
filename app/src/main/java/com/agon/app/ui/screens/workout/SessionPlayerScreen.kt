package com.agon.app.ui.screens.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agon.app.domain.model.plan.SetPrescription
import com.agon.app.domain.model.profile.Units
import com.agon.app.domain.model.training.SuggestionReason
import com.agon.app.domain.model.util.UnitConverter
import com.agon.app.ui.components.BannerTone
import com.agon.app.ui.components.EmberButton
import com.agon.app.ui.components.GhostButton
import com.agon.app.ui.components.LoopingAssetVideo
import com.agon.app.ui.components.MessageBanner
import com.agon.app.ui.components.MuscleChip
import com.agon.app.ui.components.PillRow
import com.agon.app.ui.components.SectionTitle
import com.agon.app.ui.components.TwmCard
import com.agon.app.ui.theme.Ember
import com.agon.app.ui.theme.Flame
import com.agon.app.ui.theme.Gold
import com.agon.app.ui.theme.Success
import com.agon.app.ui.theme.TrainWithMeColors
import com.agon.app.ui.theme.accent
import com.agon.app.viewmodel.WorkoutUiState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Step-by-step session player: cinematic demo hero, animated set tracker, confetti on every
 * logged set, a breathing rest-timer ring, progression coaching and technique cues.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionPlayerScreen(
    state: WorkoutUiState,
    onWeightChange: (String, Units) -> Unit,
    onRepsChange: (Int) -> Unit,
    onRpeChange: (Double?) -> Unit,
    onLogSet: () -> Unit,
    onUndoSet: () -> Unit,
    onApplySuggestion: () -> Unit,
    onSkipRest: () -> Unit,
    onExtendRest: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onGoToExercise: (Int) -> Unit,
    onSwap: () -> Unit,
    onSkipExercise: () -> Unit,
    onFinishEarly: () -> Unit,
    onExit: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = state.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var confettiTick by remember { mutableStateOf(0) }

    val overall = if (state.totalPlannedSets == 0) 0f
    else state.totalWorkingSetsLogged.toFloat() / state.totalPlannedSets
    val animatedOverall by animateFloatAsState(
        targetValue = overall.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "overall",
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit session")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.day?.title ?: "Session",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                        Text(
                            text = "Exercise ${state.currentIndex + 1} of ${state.exercises.size} · " +
                                "${state.totalWorkingSetsLogged}/${state.totalPlannedSets} sets" +
                                if (state.volumeKg > 0) " · ${format1(state.displayedVolume)} ${UnitConverter.unitLabel(state.units)}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Session options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Swap this exercise") },
                                leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                                onClick = { menuOpen = false; onSwap() },
                            )
                            DropdownMenuItem(
                                text = { Text("Skip this exercise") },
                                onClick = { menuOpen = false; onSkipExercise() },
                            )
                            DropdownMenuItem(
                                text = { Text("Finish session") },
                                onClick = { menuOpen = false; confirmFinish = true },
                            )
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { animatedOverall },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                // exercise dots — tap to jump
                if (state.exercises.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        state.exercises.forEachIndexed { index, ex ->
                            val dotColor by animateColorAsState(
                                targetValue = when {
                                    index == state.currentIndex -> MaterialTheme.colorScheme.primary
                                    ex.isDone -> TrainWithMeColors.success
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                label = "ex dot",
                            )
                            Surface(
                                onClick = { onGoToExercise(index) },
                                color = dotColor,
                                shape = CircleShape,
                                modifier = Modifier.padding(horizontal = 3.dp),
                            ) {
                                Box(
                                    Modifier.size(
                                        width = if (index == state.currentIndex) 26.dp else 9.dp,
                                        height = 9.dp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GhostButton(
                    text = "Prev",
                    onClick = onPrevious,
                    enabled = state.currentIndex > 0,
                    modifier = Modifier.weight(1f),
                )
                EmberButton(
                    text = if (state.currentIndex == state.exercises.lastIndex) "Finish" else "Next",
                    onClick = onNext,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.weight(1.3f),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (progress == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("This session has no exercises.")
            }
            return@Scaffold
        }

        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = state.currentIndex,
                transitionSpec = {
                    val forward = targetState > initialState
                    (fadeIn(tween(220)) + slideInHorizontally(tween(320)) { if (forward) it / 4 else -it / 4 }) togetherWith
                        (fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { if (forward) -it / 5 else it / 5 })
                },
                label = "exercise pager",
            ) { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnimatedVisibility(visible = state.rest != null) {
                        RestTimerRing(
                            remainingSeconds = state.rest?.remainingSeconds ?: 0,
                            totalSeconds = state.rest?.totalSeconds ?: 0,
                            onSkip = onSkipRest,
                            onExtend = onExtendRest,
                        )
                    }

                    state.message?.let {
                        MessageBanner(it, tone = BannerTone.INFO, actionLabel = "OK", onAction = onDismissMessage)
                    }
                    state.error?.let { MessageBanner(it, tone = BannerTone.ERROR) }

                    LoopingAssetVideo(
                        assetPath = progress.planned.videoAssetPath,
                        contentDescription = "Demo of ${progress.planned.name}",
                    )

                    // Title + prescription
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = progress.planned.name,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f),
                            )
                            if (progress.isSwapped) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape,
                                ) {
                                    Text(
                                        "swapped",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${progress.planned.prescription.display} · rest " +
                                "${progress.planned.prescription.restSeconds}s · tempo " +
                                "${progress.planned.prescription.tempo} · target RPE ${formatNumber(progress.planned.prescription.rpeTarget)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            progress.planned.primaryMuscles.take(2).forEach { muscle ->
                                MuscleChip(label = muscle.label, color = muscle.accent())
                            }
                        }
                        progress.planned.notes?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = TrainWithMeColors.success)
                        }
                    }

                    // ---- Animated set tracker ----
                    SetDots(
                        targetSets = progress.targetSets,
                        completedSets = progress.completedWorkingSets,
                    )

                    progress.suggestion?.let {
                        SuggestionCard(reason = it.reason, note = it.note, onApply = onApplySuggestion)
                    }

                    // ---- Logging ----
                    TwmCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Set ${progress.nextSetIndex} of ${progress.targetSets}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (progress.isDone) {
                                Surface(color = TrainWithMeColors.success.copy(alpha = 0.15f), shape = CircleShape) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = TrainWithMeColors.success, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("target hit", style = MaterialTheme.typography.labelSmall, color = TrainWithMeColors.success)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WeightInput(
                                weightKg = state.draft.weightKg,
                                units = state.units,
                                step = state.weightStep,
                                onValueChange = { onWeightChange(it, state.units) },
                                modifier = Modifier.weight(1f),
                            )
                            RepsInput(
                                reps = state.draft.reps,
                                target = progress.planned.prescription,
                                onValueChange = onRepsChange,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("How hard was that?", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        PillRow(
                            options = listOf(6.0, 7.0, 8.0, 9.0, 10.0),
                            selected = state.draft.rpe ?: -1.0,
                            onSelect = { onRpeChange(it) },
                            label = { formatNumber(it) },
                        )
                        Spacer(Modifier.height(14.dp))
                        EmberButton(
                            text = if (progress.isDone) "Log an extra set" else "Log set",
                            onClick = {
                                confettiTick++
                                onLogSet()
                            },
                            enabled = state.draft.isLoggable,
                            icon = Icons.Default.Check,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (progress.completedWorkingSets > 0) {
                            TextButton(onClick = onUndoSet, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text("Undo last set")
                            }
                        }
                    }

                    // ---- Technique ----
                    if (progress.exercise != null) {
                        SectionTitle("How to do it")
                        TwmCard {
                            progress.exercise.setup.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(10.dp))
                            }
                            progress.exercise.cues.forEachIndexed { index, cue ->
                                Row(Modifier.padding(vertical = 5.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        cue,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            if (progress.exercise.mistakes.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("Avoid", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                                        progress.exercise.mistakes.forEach { mistake ->
                                            Text("• $mistake", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Breathing: ${progress.exercise.breathing}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            progress.exercise.safetyNotes?.let {
                                Spacer(Modifier.height(6.dp))
                                MessageBanner(it, tone = BannerTone.WARNING)
                            }
                        }
                    }

                    if (progress.loggedSets.isNotEmpty()) {
                        SectionTitle("Logged this session")
                        TwmCard {
                            progress.loggedSets.filter { !it.isWarmup }.forEach { set ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape,
                                    ) {
                                        Text(
                                            "${set.setIndex}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
                                        )
                                    }
                                    Text(
                                        "${UnitConverter.formatWeight(set.weightKg, state.units)} × ${set.reps}" +
                                            (set.rpe?.let { "  @ ${formatNumber(it)}" } ?: ""),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // confetti overlay
            ConfettiBurst(tick = confettiTick, modifier = Modifier.fillMaxSize())
        }
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish the session?") },
            text = {
                Text(
                    "You have logged ${state.totalWorkingSetsLogged} of ${state.totalPlannedSets} sets. " +
                        "Finishing saves what you have done.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmFinish = false; onFinishEarly() }) { Text("Finish") }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep training") } },
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave the workout?") },
            text = { Text("Your logged sets are saved. You can resume this session later from the dashboard.") },
            confirmButton = { TextButton(onClick = { showExitDialog = false; onExit() }) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Stay") } },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Set dots
// ---------------------------------------------------------------------------------------------

@Composable
private fun SetDots(targetSets: Int, completedSets: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(targetSets) { index ->
            val done = index < completedSets
            val isCurrent = index == completedSets
            val targetColor = when {
                done -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val color by animateColorAsState(targetValue = targetColor, animationSpec = tween(220), label = "dot")
            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.15f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "dot scale",
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(color)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Set ${index + 1}"
                        stateDescription = when {
                            done -> "completed"
                            isCurrent -> "current"
                            else -> "upcoming"
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(19.dp),
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Rest timer — breathing ring
// ---------------------------------------------------------------------------------------------

@Composable
private fun RestTimerRing(
    remainingSeconds: Int,
    totalSeconds: Int,
    onSkip: () -> Unit,
    onExtend: (Int) -> Unit,
) {
    val fraction = if (totalSeconds <= 0) 0f else remainingSeconds.toFloat() / totalSeconds
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "rest",
    )
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse scale",
    )
    val urgent = remainingSeconds <= 10

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(if (urgent) 1f else pulseScale),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.25f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = if (urgent) Gold else Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (urgent) "Almost there — set up!" else "Rest & recover",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Breathe, stay warm, set up for the next set.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { onExtend(15) },
                        color = Color.White.copy(alpha = 0.22f),
                        shape = CircleShape,
                    ) {
                        Text("+15s", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                    Surface(
                        onClick = onSkip,
                        color = Color.White.copy(alpha = 0.22f),
                        shape = CircleShape,
                    ) {
                        Text("Skip", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Suggestion
// ---------------------------------------------------------------------------------------------

@Composable
private fun SuggestionCard(
    reason: SuggestionReason,
    note: String?,
    onApply: () -> Unit,
) {
    val title = when (reason) {
        SuggestionReason.FIRST_TIME -> "First time on this lift"
        SuggestionReason.ADD_LOAD -> "Progression: add load"
        SuggestionReason.ADD_REP -> "Progression: add a rep"
        SuggestionReason.HOLD_LOAD -> "Hold this load"
        SuggestionReason.CONSOLIDATE -> "Repeat this load"
        SuggestionReason.DELOAD -> "Deload"
    }
    TwmCard(containerColor = MaterialTheme.colorScheme.secondaryContainer, border = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.secondary, shape = CircleShape) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            TextButton(onClick = onApply) {
                Text("Use", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Inputs
// ---------------------------------------------------------------------------------------------

@Composable
private fun WeightInput(
    weightKg: Double,
    units: Units,
    step: Double,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val display = UnitConverter.kgToDisplay(weightKg, units)
    val text = if (display % 1.0 == 0.0) display.toInt().toString() else display.toString()
    var local by remember(weightKg, units) { mutableStateOf(text) }

    Column(modifier) {
        OutlinedTextField(
            value = local,
            onValueChange = { input ->
                local = input.filter { it.isDigit() || it == '.' }.take(6)
                onValueChange(local.ifBlank { "0" })
            },
            label = { Text("Weight") },
            suffix = { Text(UnitConverter.unitLabel(units)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
            StepperButton(icon = Icons.Default.Remove, onClick = {
                val next = (UnitConverter.kgToDisplay(weightKg, units) - step).coerceAtLeast(0.0)
                local = formatNumber(next)
                onValueChange(local)
            })
            StepperButton(icon = Icons.Default.Add, onClick = {
                val next = UnitConverter.kgToDisplay(weightKg, units) + step
                local = formatNumber(next)
                onValueChange(local)
            })
        }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.size(width = 52.dp, height = 36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RepsInput(
    reps: Int,
    target: SetPrescription,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedReps by animateFloatAsState(
        targetValue = reps.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "reps",
    )
    Column(modifier) {
        OutlinedTextField(
            value = if (reps == 0) "" else reps.toString(),
            onValueChange = { input ->
                if (input.isBlank()) onValueChange(0)
                else input.filter { it.isDigit() }.take(2).toIntOrNull()?.let(onValueChange)
            },
            label = { Text("Reps") },
            suffix = { Text("of ${target.repLabel}") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            StepperButton(icon = Icons.Default.Remove, onClick = { onValueChange((reps - 1).coerceAtLeast(0)) })
            StepperButton(icon = Icons.Default.Add, onClick = { onValueChange(reps + 1) })
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Confetti burst overlay
// ---------------------------------------------------------------------------------------------

private data class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val wobble: Float,
)

@Composable
private fun ConfettiBurst(tick: Int, modifier: Modifier = Modifier) {
    var active by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val particles = remember {
        val colors = listOf(Ember, Flame, Gold, Success, Color.White)
        List(46) {
            Particle(
                angle = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 0.55f + 0.25f,
                size = Random.nextFloat() * 7f + 3.5f,
                color = colors.random(),
                wobble = Random.nextFloat() * 60f - 30f,
            )
        }
    }
    LaunchedEffect(tick) {
        if (tick > 0) {
            active = true
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(950, easing = FastOutSlowInEasing))
            active = false
        }
    }
    if (!active) return
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height * 0.45f
        val p = progress.value
        particles.forEach { pt ->
            val dist = pt.speed * size.minDimension * p
            val rad = Math.toRadians((pt.angle + pt.wobble * p).toDouble())
            val x = cx + cos(rad).toFloat() * dist
            val y = cy + sin(rad).toFloat() * dist + p * p * size.height * 0.25f
            drawCircle(
                color = pt.color.copy(alpha = (1f - p).coerceIn(0f, 1f)),
                radius = pt.size * (1f - p * 0.5f),
                center = androidx.compose.ui.geometry.Offset(x, y),
            )
        }
    }
}

private fun formatNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

private fun format1(value: Double): String = formatNumber(value)
