package com.trainwithme.feature.workout.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainwithme.feature.workout.vm.SessionPhase
import com.trainwithme.feature.workout.vm.WorkoutEffect
import com.trainwithme.feature.workout.vm.WorkoutUiState
import com.trainwithme.feature.workout.vm.WorkoutViewModel

/**
 * Entry point for the guided session. Owns exactly three surfaces and keeps the screen awake while
 * the player is visible (docs/PLAN.md §6.4 D8).
 */
@Composable
fun WorkoutRoute(
    onExit: () -> Unit,
    onFinished: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    KeepScreenOn(enabled = state.keepScreenOn && state.phase == SessionPhase.PLAYING, view = view)
    RestCompletionFeedback(
        completionCount = state.restCompletionCount,
        enabled = state.restTimerSoundEnabled,
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                WorkoutEffect.SessionFinished -> onFinished()
                WorkoutEffect.ExitRequested -> onExit()
            }
        }
    }

    when (state.phase) {
        SessionPhase.OVERVIEW -> SessionOverviewScreen(
            state = state,
            onStart = viewModel::startSession,
            onToggleWarmup = viewModel::toggleWarmupStep,
            onBack = viewModel::requestExit,
            onDismissMessage = viewModel::dismissMessage,
        )
        SessionPhase.PLAYING -> SessionPlayerScreen(
            state = state,
            onWeightChange = viewModel::onWeightTextChange,
            onRepsChange = viewModel::onRepsChange,
            onRpeChange = viewModel::onRpeChange,
            onLogSet = viewModel::logSet,
            onUndoSet = viewModel::undoLastSet,
            onApplySuggestion = viewModel::applySuggestion,
            onSkipRest = viewModel::skipRest,
            onExtendRest = viewModel::extendRest,
            onNext = viewModel::nextExercise,
            onPrevious = viewModel::previousExercise,
            onSwap = viewModel::swapExercise,
            onSkipExercise = viewModel::skipExercise,
            onFinishEarly = viewModel::finishEarly,
            onExit = viewModel::requestExit,
            onDismissMessage = viewModel::dismissMessage,
        )
        SessionPhase.SUMMARY -> SessionSummaryScreen(
            state = state,
            onDone = viewModel::closeSession,
        )
    }
}

@Composable
private fun RestCompletionFeedback(completionCount: Int, enabled: Boolean) {
    val haptic = LocalHapticFeedback.current
    val toneGenerator = androidx.compose.runtime.remember {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70) }.getOrNull()
    }
    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator?.release() }
    }
    LaunchedEffect(completionCount) {
        if (completionCount > 0 && enabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 220)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean, view: android.view.View) {
    LaunchedEffect(enabled, view) {
        view.keepScreenOn = enabled
    }
}

@Composable
fun WorkoutRoutePlaceholder(state: WorkoutUiState, modifier: Modifier = Modifier) {
    SessionOverviewScreen(
        state = state,
        onStart = {},
        onToggleWarmup = {},
        onBack = {},
        onDismissMessage = {},
    )
}
