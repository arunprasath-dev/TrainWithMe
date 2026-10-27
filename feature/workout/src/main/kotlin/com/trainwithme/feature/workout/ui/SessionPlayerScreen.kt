package com.trainwithme.feature.workout.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import com.trainwithme.core.designsystem.component.BannerTone
import com.trainwithme.core.designsystem.component.MessageBanner
import com.trainwithme.core.designsystem.component.PillRow
import com.trainwithme.core.designsystem.component.PrimaryButton
import com.trainwithme.core.designsystem.component.SecondaryButton
import com.trainwithme.core.designsystem.component.SectionTitle
import com.trainwithme.core.designsystem.component.TwmCard
import com.trainwithme.core.designsystem.component.LoopingAssetVideo
import com.trainwithme.core.model.plan.SetPrescription
import com.trainwithme.core.model.profile.Units
import com.trainwithme.core.model.training.SuggestionReason
import com.trainwithme.core.model.util.UnitConverter
import com.trainwithme.feature.workout.vm.WorkoutUiState

/**
 * Step-by-step session player: video, cues, set logging, rest timer.
 *
 * Layout note: the primary actions live in the bottom bar (one-thumb reach), the video and the cues
 * scroll in the body, so a 200 % font scale never hides the "Log set" button.
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
                                "${state.totalWorkingSetsLogged}/${state.totalPlannedSets} sets",
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
                val overall = if (state.totalPlannedSets == 0) {
                    0f
                } else {
                    state.totalWorkingSetsLogged.toFloat() / state.totalPlannedSets
                }
                LinearProgressIndicator(
                    progress = { overall.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = "Previous",
                        onClick = onPrevious,
                        enabled = state.currentIndex > 0,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = if (state.currentIndex == state.exercises.lastIndex) "Finish" else "Next exercise",
                        onClick = onNext,
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        modifier = Modifier.weight(1.3f),
                    )
                }
            }
        },
    ) { padding ->
        if (progress == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("This session has no exercises.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(visible = state.rest != null) {
                RestTimerCard(
                    remainingSeconds = state.rest?.remainingSeconds ?: 0,
                    totalSeconds = state.rest?.totalSeconds ?: 0,
                    onSkip = onSkipRest,
                    onExtend = onExtendRest,
                )
            }

            state.message?.let { MessageBanner(it, tone = BannerTone.INFO, actionLabel = "OK", onAction = onDismissMessage) }
            state.error?.let { MessageBanner(it, tone = BannerTone.ERROR) }

            LoopingAssetVideo(
                assetPath = progress.planned.videoAssetPath,
                contentDescription = "Demo of ${progress.planned.name}",
            )

            Column {
                Row(verticalAlignment = Alignment.Bottom) {
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Text(
                    text = "${progress.planned.prescription.display} · rest " +
                        "${progress.planned.prescription.restSeconds}s · tempo " +
                        "${progress.planned.prescription.tempo} · RPE ${formatNumber(progress.planned.prescription.rpeTarget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                progress.planned.notes?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            }

            // ---- Set tracker ----
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(progress.targetSets) { index ->
                    val done = index < progress.completedWorkingSets
                    val isCurrent = index == progress.completedWorkingSets
                    val targetSetColor = when {
                        done -> MaterialTheme.colorScheme.primary
                        isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val setColor by animateColorAsState(
                        targetValue = targetSetColor,
                        animationSpec = tween(durationMillis = 180),
                        label = "set completion",
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(setColor)
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
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            progress.suggestion?.let { SuggestionCard(it.reason, it.note, onApply = onApplySuggestion) }

            // ---- Logging ----
            TwmCard {
                SectionTitle("Set ${progress.nextSetIndex} of ${progress.targetSets}")
                Spacer(Modifier.height(10.dp))
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    text = if (progress.isDone) "Log an extra set" else "Log set",
                    onClick = onLogSet,
                    enabled = state.draft.isLoggable,
                    icon = Icons.Default.Check,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (progress.completedWorkingSets > 0) {
                    TextButton(onClick = onUndoSet) { Text("Undo last set") }
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
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.size(10.dp))
                            Text(cue, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (progress.exercise.mistakes.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Avoid", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                        progress.exercise.mistakes.forEach { mistake ->
                            Text("• $mistake", style = MaterialTheme.typography.bodyMedium)
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
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Set ${set.setIndex}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${UnitConverter.formatWeight(set.weightKg, state.units)} × ${set.reps}" +
                                    (set.rpe?.let { " @ ${formatNumber(it)}" } ?: ""),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
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

@Composable
private fun RestTimerCard(
    remainingSeconds: Int,
    totalSeconds: Int,
    onSkip: () -> Unit,
    onExtend: (Int) -> Unit,
) {
    val fraction = if (totalSeconds <= 0) 0f else remainingSeconds.toFloat() / totalSeconds
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 240),
        label = "rest timer progress",
    )
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Rest", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                TextButton(onClick = { onExtend(15) }) { Text("+15s") }
                TextButton(onClick = onSkip) { Text("Skip") }
            }
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
            )
            Text(
                "Breathe, stay warm, and set up for the next set.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

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
    TwmCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onApply) { Text("Use") }
        }
    }
}

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
                onValueChange(local)
            },
            label = { Text("Weight") },
            suffix = { Text(UnitConverter.unitLabel(units)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
            listOf(-step, step).forEach { delta ->
                TextButton(
                    onClick = {
                        val next = (UnitConverter.kgToDisplay(weightKg, units) + delta).coerceAtLeast(0.0)
                        local = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                        onValueChange(local)
                    },
                ) {
                    Text(if (delta < 0) "${formatNumber(-delta)}−" else "+${formatNumber(delta)}")
                }
            }
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
    Column(modifier) {
        OutlinedTextField(
            value = if (reps == 0) "" else reps.toString(),
            onValueChange = { input -> input.filter { it.isDigit() }.take(2).toIntOrNull()?.let(onValueChange) },
            label = { Text("Reps") },
            suffix = { Text("of ${target.repLabel}") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            TextButton(onClick = { onValueChange((reps - 1).coerceAtLeast(0)) }) { Text("−1") }
            TextButton(onClick = { onValueChange(reps + 1) }) { Text("+1") }
        }
    }
}

private fun formatNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
