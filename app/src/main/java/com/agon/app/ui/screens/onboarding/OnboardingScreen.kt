package com.agon.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agon.app.R
import com.agon.app.domain.model.profile.ActivityLevel
import com.agon.app.domain.model.profile.EquipmentAccess
import com.agon.app.domain.model.profile.ExperienceLevel
import com.agon.app.domain.model.profile.Goal
import com.agon.app.domain.model.profile.IssueCode
import com.agon.app.domain.model.profile.ProfileField
import com.agon.app.domain.model.profile.Sex
import com.agon.app.domain.model.profile.UserProfile
import com.agon.app.ui.components.BannerTone
import com.agon.app.ui.components.EmberButton
import com.agon.app.ui.components.MessageBanner
import com.agon.app.ui.components.OptionChips
import com.agon.app.ui.components.TwmCard
import com.agon.app.ui.theme.Ember
import com.agon.app.ui.theme.TrainWithMeColors
import com.agon.app.viewmodel.AppViewModelFactory
import com.agon.app.viewmodel.OnboardingEffect
import com.agon.app.viewmodel.OnboardingStep
import com.agon.app.viewmodel.OnboardingUiState
import com.agon.app.viewmodel.OnboardingViewModel

@Composable
fun OnboardingRoute(
    factory: AppViewModelFactory,
    onFinished: () -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(factory = factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.Finished -> onFinished()
                OnboardingEffect.Cancelled -> onCancelled()
            }
        }
    }

    OnboardingContent(
        state = state,
        onNext = viewModel::next,
        onBack = viewModel::back,
        onEditStep = viewModel::goToStep,
        onNameChange = viewModel::onNameChange,
        onSexChange = viewModel::onSexChange,
        onAgeChange = viewModel::onAgeChange,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onBodyFatChange = viewModel::onBodyFatChange,
        onNeckChange = viewModel::onNeckChange,
        onWaistChange = viewModel::onWaistChange,
        onHipChange = viewModel::onHipChange,
        onChestChange = viewModel::onChestChange,
        onArmChange = viewModel::onArmChange,
        onThighChange = viewModel::onThighChange,
        onExperienceChange = viewModel::onExperienceChange,
        onActivityChange = viewModel::onActivityChange,
        onGoalChange = viewModel::onGoalChange,
        onEquipmentChange = viewModel::onEquipmentChange,
        onDaysChange = viewModel::onDaysChange,
        onMinutesChange = viewModel::onMinutesChange,
        onDisclaimerAccepted = viewModel::onDisclaimerAccepted,
        modifier = modifier,
    )
}

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onEditStep: (OnboardingStep) -> Unit,
    onNameChange: (String) -> Unit,
    onSexChange: (Sex) -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onBodyFatChange: (String) -> Unit,
    onNeckChange: (String) -> Unit,
    onWaistChange: (String) -> Unit,
    onHipChange: (String) -> Unit,
    onChestChange: (String) -> Unit,
    onArmChange: (String) -> Unit,
    onThighChange: (String) -> Unit,
    onExperienceChange: (ExperienceLevel) -> Unit,
    onActivityChange: (ActivityLevel) -> Unit,
    onGoalChange: (Goal) -> Unit,
    onEquipmentChange: (EquipmentAccess) -> Unit,
    onDaysChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onDisclaimerAccepted: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stepMeta = remember(state.step) { stepMeta(state.step) }
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "wizard progress",
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "STEP ${state.stepIndex + 1} OF ${OnboardingStep.ordered.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.isEditingExistingProfile) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                "EDITING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EmberButton(
                    text = when {
                        state.step.isLast && state.isEditingExistingProfile -> "Save changes"
                        state.step.isLast -> "Build my plan"
                        else -> "Continue"
                    },
                    onClick = onNext,
                    loading = state.isSaving,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = state.stepIndex,
            transitionSpec = {
                val forward = targetState > initialState
                (fadeIn(tween(200)) + slideInHorizontally(tween(300)) { if (forward) it / 3 else -it / 3 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { if (forward) -it / 4 else it / 4 })
            },
            label = "wizard step",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StepHeader(icon = stepMeta.icon, kicker = stepMeta.kicker, title = stepMeta.title)

                state.failureMessage?.let {
                    MessageBanner(it, tone = BannerTone.ERROR)
                }

                when (state.step) {
                    OnboardingStep.WELCOME -> WelcomeStep(state, onDisclaimerAccepted)
                    OnboardingStep.BODY -> BodyStep(
                        state, onNameChange, onSexChange, onAgeChange,
                        onHeightChange, onWeightChange, onBodyFatChange,
                    )
                    OnboardingStep.MEASUREMENTS -> MeasurementsStep(
                        state, onNeckChange, onWaistChange, onHipChange,
                        onChestChange, onArmChange, onThighChange,
                    )
                    OnboardingStep.EXPERIENCE -> ExperienceStep(state, onExperienceChange, onActivityChange)
                    OnboardingStep.GOAL -> GoalStep(state, onGoalChange)
                    OnboardingStep.SCHEDULE -> ScheduleStep(state, onEquipmentChange, onDaysChange, onMinutesChange)
                    OnboardingStep.REVIEW -> ReviewStep(state, onEditStep)
                }

                if (state.warnings.isNotEmpty() && state.step != OnboardingStep.WELCOME) {
                    state.warnings.take(2).forEach { warning ->
                        MessageBanner(warningText(warning.code), tone = BannerTone.WARNING)
                    }
                }
            }
        }
    }
}

private data class StepMeta(val icon: ImageVector, val kicker: String, val title: String)

private fun stepMeta(step: OnboardingStep): StepMeta = when (step) {
    OnboardingStep.WELCOME -> StepMeta(Icons.Default.AutoAwesome, "WELCOME", "Your coach is ready")
    OnboardingStep.BODY -> StepMeta(Icons.Default.Person, "ABOUT YOU", "Tell us about your body")
    OnboardingStep.MEASUREMENTS -> StepMeta(Icons.Default.MonitorWeight, "MEASUREMENTS", "Tape measurements")
    OnboardingStep.EXPERIENCE -> StepMeta(Icons.Default.Timeline, "TRAINING HISTORY", "How do you train?")
    OnboardingStep.GOAL -> StepMeta(Icons.Default.EmojiEvents, "YOUR GOAL", "What are we chasing?")
    OnboardingStep.SCHEDULE -> StepMeta(Icons.Default.CalendarMonth, "YOUR WEEK", "When can you train?")
    OnboardingStep.REVIEW -> StepMeta(Icons.Default.Check, "REVIEW", "Your programme preview")
}

@Composable
private fun StepHeader(icon: ImageVector, kicker: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(10.dp)
                    .size(24.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(kicker, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Steps
// ---------------------------------------------------------------------------------------------

@Composable
private fun WelcomeStep(state: OnboardingUiState, onAccepted: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.onboarding_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                    ),
                ),
        )
        Text(
            "8 weeks. Built around your body, your goal, your week.",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }
    Text(
        "TrainWithMe builds your gym programme around your body, your goal and your week — then coaches you through every set.",
        style = MaterialTheme.typography.bodyLarge,
    )
    TwmCard {
        FeatureRow(Icons.Default.FitnessCenter, "8-week plan with deloads", "Built from your body composition")
        FeatureRow(Icons.Default.AutoAwesome, "Guided sessions", "Demo clips, cues and a rest timer")
        FeatureRow(Icons.Default.Timeline, "Automatic progression", "Load and reps adjust when you're ready")
        FeatureRow(Icons.Default.Restaurant, "Nutrition targets", "Calories and protein with the maths shown")
    }
    TwmCard {
        Text("Before we start", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "This app is a training guide, not medical advice. If you have a heart condition, an injury, are pregnant, or have been told to avoid exercise, talk to a doctor first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(true to "I understand", false to "Not yet").forEach { (value, label) ->
                val selected = state.agreedToDisclaimer == value
                Surface(
                    onClick = { onAccepted(value) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 13.dp)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        if (state.errors.containsKey(ProfileField.DISCLAIMER)) {
            Spacer(Modifier.height(8.dp))
            MessageBanner("Please confirm you understand before continuing.", tone = BannerTone.WARNING)
        }
    }
    Text(
        "All data stays on this device. No account, no cloud, no ads.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BodyStep(
    state: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onSexChange: (Sex) -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onBodyFatChange: (String) -> Unit,
) {
    WizardField(
        value = state.name,
        onValueChange = onNameChange,
        label = "What should we call you?",
        placeholder = "Your name",
        error = errorText(ProfileField.NAME, state.errors),
        keyboardType = KeyboardType.Text,
    )
    Text("Biological sex (used only for calorie maths)", style = MaterialTheme.typography.titleSmall)
    OptionChips(
        options = Sex.entries,
        selected = state.sex,
        onSelect = onSexChange,
        label = { if (it == Sex.MALE) "Male" else "Female" },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WizardField(
            value = state.age,
            onValueChange = onAgeChange,
            label = "Age",
            placeholder = "28",
            suffix = "yrs",
            error = errorText(ProfileField.AGE, state.errors),
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        WizardField(
            value = state.heightCm,
            onValueChange = onHeightChange,
            label = "Height",
            placeholder = "178",
            suffix = "cm",
            error = errorText(ProfileField.HEIGHT, state.errors),
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WizardField(
            value = state.weightKg,
            onValueChange = onWeightChange,
            label = "Weight",
            placeholder = "75",
            suffix = "kg",
            error = errorText(ProfileField.WEIGHT, state.errors),
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
        )
        WizardField(
            value = state.bodyFatPercent,
            onValueChange = onBodyFatChange,
            label = "Body fat (optional)",
            placeholder = "—",
            suffix = "%",
            error = errorText(ProfileField.BODY_FAT, state.errors),
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
        )
    }
    if (state.metrics != null) {
        LiveMetricsCard(state)
    }
}

@Composable
private fun MeasurementsStep(
    state: OnboardingUiState,
    onNeckChange: (String) -> Unit,
    onWaistChange: (String) -> Unit,
    onHipChange: (String) -> Unit,
    onChestChange: (String) -> Unit,
    onArmChange: (String) -> Unit,
    onThighChange: (String) -> Unit,
) {
    Text(
        "Optional, but a waist + neck measurement lets us estimate body fat and track shape changes beyond the scale.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WizardField(
            value = state.waistCm, onValueChange = onWaistChange, label = "Waist", placeholder = "—",
            suffix = "cm", error = errorText(ProfileField.WAIST, state.errors),
            keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f),
        )
        WizardField(
            value = state.neckCm, onValueChange = onNeckChange, label = "Neck", placeholder = "—",
            suffix = "cm", error = errorText(ProfileField.NECK, state.errors),
            keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WizardField(
            value = state.hipCm, onValueChange = onHipChange, label = "Hips", placeholder = "—",
            suffix = "cm", error = errorText(ProfileField.HIP, state.errors),
            keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f),
        )
        WizardField(
            value = state.chestCm, onValueChange = onChestChange, label = "Chest", placeholder = "—",
            suffix = "cm", error = null,
            keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WizardField(
            value = state.armCm, onValueChange = onArmChange, label = "Arm", placeholder = "—",
            suffix = "cm", error = null,
            keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f),
        )
        WizardField(
            value = state.thighCm, onValueChange = onThighChange, label = "Thigh", placeholder = "—",
            suffix = "cm", error = null,
            keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f),
        )
    }
    if (state.metrics?.bodyFatPercent != null) {
        TwmCard(containerColor = MaterialTheme.colorScheme.secondaryContainer, border = false) {
            Text(
                "Estimated body fat: ${state.metrics.bodyFatPercent}% (US Navy method)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "This powers your calorie and protein targets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
            )
        }
    } else {
        MessageBanner(
            "Skip everything here if you like — you can log measurements later from Progress.",
            tone = BannerTone.INFO,
        )
    }
}

@Composable
private fun ExperienceStep(
    state: OnboardingUiState,
    onExperienceChange: (ExperienceLevel) -> Unit,
    onActivityChange: (ActivityLevel) -> Unit,
) {
    Text("How long have you been training?", style = MaterialTheme.typography.titleSmall)
    OptionChips(
        options = ExperienceLevel.entries,
        selected = state.experience,
        onSelect = onExperienceChange,
        label = { it.label },
        hint = { it.hint },
    )
    errorText(ProfileField.EXPERIENCE, state.errors)?.let {
        MessageBanner(it, tone = BannerTone.WARNING)
    }
    Spacer(Modifier.height(4.dp))
    Text("How active is a normal day, outside the gym?", style = MaterialTheme.typography.titleSmall)
    OptionChips(
        options = ActivityLevel.entries,
        selected = state.activityLevel,
        onSelect = onActivityChange,
        label = { it.label },
        hint = { it.hint },
    )
    errorText(ProfileField.ACTIVITY_LEVEL, state.errors)?.let {
        MessageBanner(it, tone = BannerTone.WARNING)
    }
}

@Composable
private fun GoalStep(state: OnboardingUiState, onGoalChange: (Goal) -> Unit) {
    Text("Pick one — it drives your split, rep ranges and calories.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    OptionChips(
        options = Goal.entries,
        selected = state.goal,
        onSelect = onGoalChange,
        label = { it.label },
        hint = { it.hint },
    )
    errorText(ProfileField.GOAL, state.errors)?.let {
        MessageBanner(it, tone = BannerTone.WARNING)
    }
}

@Composable
private fun ScheduleStep(
    state: OnboardingUiState,
    onEquipmentChange: (EquipmentAccess) -> Unit,
    onDaysChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
) {
    Text("What can you train with?", style = MaterialTheme.typography.titleSmall)
    OptionChips(
        options = EquipmentAccess.entries,
        selected = state.equipmentAccess,
        onSelect = onEquipmentChange,
        label = { it.label },
        hint = { it.hint },
    )
    errorText(ProfileField.EQUIPMENT, state.errors)?.let {
        MessageBanner(it, tone = BannerTone.WARNING)
    }
    TwmCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Days per week", style = MaterialTheme.typography.titleMedium)
            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                Text(
                    "${state.daysPerWeek}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }
        }
        Slider(
            value = state.daysPerWeek.toFloat(),
            onValueChange = { onDaysChange(it.toInt()) },
            valueRange = UserProfile.MIN_DAYS.toFloat()..UserProfile.MAX_DAYS.toFloat(),
            steps = UserProfile.MAX_DAYS - UserProfile.MIN_DAYS - 1,
        )
        Text(
            splitHint(state.daysPerWeek),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    TwmCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Minutes per session", style = MaterialTheme.typography.titleMedium)
            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                Text(
                    "${state.sessionMinutes}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }
        }
        Slider(
            value = state.sessionMinutes.toFloat(),
            onValueChange = { onMinutesChange((it / 5).toInt() * 5) },
            valueRange = UserProfile.MIN_SESSION_MINUTES.toFloat()..UserProfile.MAX_SESSION_MINUTES.toFloat(),
            steps = (UserProfile.MAX_SESSION_MINUTES - UserProfile.MIN_SESSION_MINUTES) / 5 - 1,
        )
        Text(
            "Warm-up, work and cool-down fit inside this budget — the plan trims sets to fit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun splitHint(days: Int): String = when (days) {
    2 -> "Full body ×2 — every muscle trained twice a week."
    3 -> "Full body ×3 — high frequency with rest days between."
    4 -> "Upper / Lower ×2 — each muscle trained twice weekly."
    5 -> "Push / Pull / Legs + Upper / Lower blend."
    else -> "Push / Pull / Legs ×2 — two full rotations."
}

@Composable
private fun ReviewStep(state: OnboardingUiState, onEditStep: (OnboardingStep) -> Unit) {
    state.metrics?.let { metrics ->
        TwmCard(containerColor = MaterialTheme.colorScheme.primaryContainer, border = false) {
            Text(
                "YOUR NUMBERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ReviewStat("BMI", String.format(java.util.Locale.US, "%.1f", metrics.bmi))
                state.nutrition?.let { ReviewStat("Target", "${it.calories} kcal") }
                state.nutrition?.let { ReviewStat("Protein", "${it.proteinG} g") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${state.splitName} · ${state.daysPerWeek} days/week · ~${state.sessionMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
    ReviewCard(title = "Body", summary = bodySummary(state), onEdit = { onEditStep(OnboardingStep.BODY) })
    ReviewCard(title = "Goal & experience", summary = goalSummary(state), onEdit = { onEditStep(OnboardingStep.GOAL) })
    ReviewCard(title = "Schedule", summary = scheduleSummary(state), onEdit = { onEditStep(OnboardingStep.SCHEDULE) })
    if (state.isUnderweight) {
        MessageBanner(
            "Your BMI is below the healthy range — the plan will prioritise fuelling and gradual loading.",
            tone = BannerTone.INFO,
        )
    }
}

@Composable
private fun ReviewStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun ReviewCard(title: String, summary: String, onEdit: () -> Unit) {
    TwmCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Edit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun bodySummary(state: OnboardingUiState): String =
    "${state.name.ifBlank { "—" }} · ${state.age.ifBlank { "—" }} yrs · ${state.heightCm.ifBlank { "—" }} cm · ${state.weightKg.ifBlank { "—" }} kg"

private fun goalSummary(state: OnboardingUiState): String =
    "${state.goal?.label ?: "—"} · ${state.experience?.label ?: "—"} · ${state.activityLevel?.label ?: "—"}"

private fun scheduleSummary(state: OnboardingUiState): String =
    "${state.equipmentAccess?.label ?: "—"} · ${state.daysPerWeek} days · ${state.sessionMinutes} min"

@Composable
private fun LiveMetricsCard(state: OnboardingUiState) {
    val metrics = state.metrics ?: return
    TwmCard(containerColor = MaterialTheme.colorScheme.secondaryContainer, border = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text(
                "Live preview",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "BMI ${String.format(java.util.Locale.US, "%.1f", metrics.bmi)} · " +
                "BMR ${metrics.bmrKcal} kcal · TDEE ${metrics.tdeeKcal} kcal" +
                (state.nutrition?.let { " · target ${it.calories} kcal, ${it.proteinG} g protein" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Fields + error text
// ---------------------------------------------------------------------------------------------

@Composable
private fun WizardField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            suffix = suffix?.let { { Text(it) } },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun errorText(field: ProfileField, errors: Map<ProfileField, IssueCode>): String? {
    return when (errors[field]) {
        null -> null
        IssueCode.REQUIRED -> "Required"
        IssueCode.OUT_OF_RANGE -> rangeHint(field)
        IssueCode.DISCLAIMER_REQUIRED -> "Please confirm to continue"
        else -> "Check this value"
    }
}

private fun rangeHint(field: ProfileField): String = when (field) {
    ProfileField.AGE -> "Age ${UserProfile.MIN_AGE}–${UserProfile.MAX_AGE}"
    ProfileField.HEIGHT -> "${UserProfile.MIN_HEIGHT_CM.toInt()}–${UserProfile.MAX_HEIGHT_CM.toInt()} cm"
    ProfileField.WEIGHT -> "${UserProfile.MIN_WEIGHT_KG.toInt()}–${UserProfile.MAX_WEIGHT_KG.toInt()} kg"
    ProfileField.BODY_FAT -> "${UserProfile.MIN_BODY_FAT.toInt()}–${UserProfile.MAX_BODY_FAT.toInt()}%"
    ProfileField.WAIST -> "40–200 cm"
    ProfileField.NECK -> "20–70 cm"
    ProfileField.HIP -> "50–200 cm"
    else -> "Out of range"
}

private fun warningText(code: IssueCode): String = when (code) {
    IssueCode.UNDERWEIGHT_WITH_FAT_LOSS -> "Your BMI is below the healthy range and your goal is fat loss — consider fuelling up and training for strength instead."
    IssueCode.MINOR_NEEDS_SUPERVISION -> "You're under 18 — train with supervision and keep loads moderate."
    IssueCode.VERY_HIGH_BODY_FAT -> "With higher body fat, steady habits beat aggressive cuts — the plan keeps the deficit moderate."
    IssueCode.WAIST_TO_HEIGHT_RISK -> "Your waist-to-height ratio is above 0.5 — regular training and daily steps will move this."
    IssueCode.AGGRESSIVE_RATE -> "Your target rate of weight change is aggressive — slower is safer and easier to keep."
    else -> "Double-check this value."
}
