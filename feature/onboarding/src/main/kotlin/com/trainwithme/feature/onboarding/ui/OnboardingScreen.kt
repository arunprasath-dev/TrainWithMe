package com.trainwithme.feature.onboarding.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainwithme.core.designsystem.component.DetailRow
import com.trainwithme.core.designsystem.component.MessageBanner
import com.trainwithme.core.designsystem.component.BannerTone
import com.trainwithme.core.designsystem.component.OptionChips
import com.trainwithme.core.designsystem.component.PillRow
import com.trainwithme.core.designsystem.component.PrimaryButton
import com.trainwithme.core.designsystem.component.SectionTitle
import com.trainwithme.core.designsystem.component.SecondaryButton
import com.trainwithme.core.designsystem.component.TwmCard
import com.trainwithme.core.designsystem.theme.TrainWithMeTheme
import com.trainwithme.core.model.profile.ActivityLevel
import com.trainwithme.core.model.profile.EquipmentAccess
import com.trainwithme.core.model.profile.ExperienceLevel
import com.trainwithme.core.model.profile.Goal
import com.trainwithme.core.model.profile.IssueCode
import com.trainwithme.core.model.profile.ProfileField
import com.trainwithme.core.model.profile.Sex
import com.trainwithme.core.model.profile.UserProfile
import com.trainwithme.feature.onboarding.vm.OnboardingEffect
import com.trainwithme.feature.onboarding.vm.OnboardingStep
import com.trainwithme.feature.onboarding.vm.OnboardingUiState
import com.trainwithme.feature.onboarding.vm.OnboardingViewModel
import com.trainwithme.feature.onboarding.vm.withPreview

/**
 * Onboarding wizard. Stateless [OnboardingContent] is previewable and testable; the screen only
 * wires the ViewModel and one-shot effects.
 */
@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    onCancelled: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
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
        onNext = viewModel::next,
        onBack = viewModel::back,
        onEditStep = viewModel::goToStep,
    )
}

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
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
    onNext: () -> Unit,
    onBack: () -> Unit,
    onEditStep: (OnboardingStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.step.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${state.stepIndex + 1}/${OnboardingStep.ordered.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.canGoBack) {
                        SecondaryButton(
                            text = if (state.isEditingExistingProfile) "Cancel" else "Back",
                            onClick = onBack,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            modifier = Modifier.weight(1f),
                        )
                    } else if (state.isEditingExistingProfile) {
                        SecondaryButton(text = "Cancel", onClick = onBack, modifier = Modifier.weight(1f))
                    }
                    PrimaryButton(
                        text = when {
                            state.step.isLast && !state.isEditingExistingProfile -> "Build my plan"
                            state.step.isLast -> "Save profile"
                            else -> "Continue"
                        },
                        onClick = onNext,
                        enabled = !state.isSaving,
                        icon = if (state.step.isLast) null else Icons.Default.ArrowForward,
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.failureMessage?.let { MessageBanner(it, tone = BannerTone.ERROR) }

            when (state.step) {
                OnboardingStep.WELCOME -> WelcomeStep(state, onDisclaimerAccepted)
                OnboardingStep.BODY -> BodyStep(
                    state = state,
                    onNameChange = onNameChange,
                    onSexChange = onSexChange,
                    onAgeChange = onAgeChange,
                    onHeightChange = onHeightChange,
                    onWeightChange = onWeightChange,
                    onBodyFatChange = onBodyFatChange,
                )
                OnboardingStep.MEASUREMENTS -> MeasurementsStep(
                    state = state,
                    onNeckChange = onNeckChange,
                    onWaistChange = onWaistChange,
                    onHipChange = onHipChange,
                    onChestChange = onChestChange,
                    onArmChange = onArmChange,
                    onThighChange = onThighChange,
                )
                OnboardingStep.EXPERIENCE -> ExperienceStep(state, onExperienceChange, onActivityChange)
                OnboardingStep.GOAL -> GoalStep(state, onGoalChange)
                OnboardingStep.SCHEDULE -> ScheduleStep(
                    state = state,
                    onEquipmentChange = onEquipmentChange,
                    onDaysChange = onDaysChange,
                    onMinutesChange = onMinutesChange,
                )
                OnboardingStep.REVIEW -> ReviewStep(state, onEditStep)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WelcomeStep(state: OnboardingUiState, onAccepted: (Boolean) -> Unit) {
    Text(
        "TrainWithMe builds your gym programme around your body, your goal and your week — then coaches " +
            "you through every set.",
        style = MaterialTheme.typography.bodyLarge,
    )
    TwmCard {
        SectionTitle("What you'll get")
        Spacer(Modifier.height(8.dp))
        Bullet("An 8-week plan with deloads, built from your body composition")
        Bullet("A guided session player with a demo clip and step-by-step cues")
        Bullet("Automatic progression: we add weight or reps when you're ready")
        Bullet("Calorie and protein targets with the maths shown")
    }
    TwmCard {
        SectionTitle("Before we start")
        Spacer(Modifier.height(8.dp))
        Text(
            "This app is a training guide, not medical advice. If you have a heart condition, an " +
                "injury, are pregnant, or have been told to avoid exercise, talk to a doctor first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        PillRow(
            options = listOf(true, false),
            selected = state.agreedToDisclaimer,
            onSelect = onAccepted,
            label = { if (it) "I understand" else "Not yet" },
        )
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
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("•  ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
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
    Text(
        "These four numbers set your calorie needs and starting loads.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TwmTextField(
        value = state.name,
        onValueChange = onNameChange,
        label = "Name",
        isError = state.errors.containsKey(ProfileField.NAME),
        errorText = "Please enter your name",
    )
    SectionTitle("Biological sex (used for metabolic estimates)")
    PillRow(
        options = Sex.entries,
        selected = state.sex,
        onSelect = onSexChange,
        label = { if (it == Sex.MALE) "Male" else "Female" },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TwmTextField(
            value = state.age,
            onValueChange = onAgeChange,
            label = "Age",
            suffix = "years",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
            isError = state.errors.containsKey(ProfileField.AGE),
        )
        TwmTextField(
            value = state.heightCm,
            onValueChange = onHeightChange,
            label = "Height",
            suffix = "cm",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
            isError = state.errors.containsKey(ProfileField.HEIGHT),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TwmTextField(
            value = state.weightKg,
            onValueChange = onWeightChange,
            label = "Weight",
            suffix = "kg",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
            isError = state.errors.containsKey(ProfileField.WEIGHT),
        )
        TwmTextField(
            value = state.bodyFatPercent,
            onValueChange = onBodyFatChange,
            label = "Body fat (optional)",
            suffix = "%",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
            isError = state.errors.containsKey(ProfileField.BODY_FAT),
        )
    }
    Text(
        "Body fat is optional. If you skip it we estimate from your measurements, or fall back to " +
            "height/weight/age formulas.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    state.metrics?.let { metrics ->
        TwmCard {
            DetailRow("BMI", "${metrics.bmi}")
            DetailRow("Estimated daily burn", "${metrics.tdeeKcal} kcal")
        }
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
        "Optional, but worth it: tape measurements improve your body-fat estimate and let the app " +
            "show where you are changing shape, not just what the scale says.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TwmTextField(state.neckCm, onNeckChange, "Neck", "cm", modifier = Modifier.weight(1f))
        TwmTextField(state.waistCm, onWaistChange, "Waist", "cm", modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TwmTextField(state.hipCm, onHipChange, "Hip", "cm", modifier = Modifier.weight(1f))
        TwmTextField(state.chestCm, onChestChange, "Chest", "cm", modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TwmTextField(state.armCm, onArmChange, "Arm", "cm", modifier = Modifier.weight(1f))
        TwmTextField(state.thighCm, onThighChange, "Thigh", "cm", modifier = Modifier.weight(1f))
    }
    state.metrics?.let { metrics ->
        if (metrics.bodyFatPercent != null) {
            TwmCard {
                DetailRow(
                    label = if (metrics.bodyFatIsEstimated) "Estimated body fat" else "Body fat",
                    value = "${metrics.bodyFatPercent}%",
                )
                if (metrics.waistToHeightRatio != null) {
                    DetailRow("Waist-to-height", "${metrics.waistToHeightRatio}")
                }
            }
        }
    }
    MessageBanner(
        "Measure at the same time of day, relaxed, without pulling the tape tight.",
        tone = BannerTone.INFO,
    )
}

@Composable
private fun ExperienceStep(
    state: OnboardingUiState,
    onExperienceChange: (ExperienceLevel) -> Unit,
    onActivityChange: (ActivityLevel) -> Unit,
) {
    SectionTitle("How long have you been training?")
    OptionChips(
        options = ExperienceLevel.entries.toList(),
        selected = state.experience,
        onSelect = onExperienceChange,
        label = { it.label },
        supportingText = { it.hint },
    )
    Spacer(Modifier.height(8.dp))
    SectionTitle("How active are you outside the gym?")
    OptionChips(
        options = ActivityLevel.entries.toList(),
        selected = state.activityLevel,
        onSelect = onActivityChange,
        label = { it.label },
        supportingText = { it.hint },
    )
}

@Composable
private fun GoalStep(state: OnboardingUiState, onGoalChange: (Goal) -> Unit) {
    SectionTitle("What are you training for?")
    OptionChips(
        options = Goal.entries.toList(),
        selected = state.goal,
        onSelect = onGoalChange,
        label = { it.label },
        supportingText = { it.hint },
    )
    state.nutrition?.let { nutrition ->
        TwmCard {
            SectionTitle("Daily targets for this goal")
            Spacer(Modifier.height(8.dp))
            DetailRow("Calories", "${nutrition.calories} kcal")
            DetailRow("Protein", "${nutrition.proteinG} g")
            DetailRow("Fat", "${nutrition.fatG} g")
            DetailRow("Carbs", "${nutrition.carbsG} g")
        }
    }
}

@Composable
private fun ScheduleStep(
    state: OnboardingUiState,
    onEquipmentChange: (EquipmentAccess) -> Unit,
    onDaysChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
) {
    SectionTitle("What equipment can you use?")
    OptionChips(
        options = EquipmentAccess.entries.toList(),
        selected = state.equipmentAccess,
        onSelect = onEquipmentChange,
        label = { it.label },
        supportingText = { it.hint },
    )
    Spacer(Modifier.height(8.dp))
    SectionTitle("Days per week: ${state.daysPerWeek}")
    PillRow(
        options = ((UserProfile.MIN_DAYS..UserProfile.MAX_DAYS)).toList(),
        selected = state.daysPerWeek,
        onSelect = onDaysChange,
        label = { it.toString() },
    )
    Spacer(Modifier.height(8.dp))
    SectionTitle("Minutes per session: ${state.sessionMinutes}")
    PillRow(
        options = listOf(30, 45, 60, 75, 90),
        selected = state.sessionMinutes,
        onSelect = onMinutesChange,
        label = { "$it" },
    )
    state.splitName?.let { split ->
        TwmCard {
            SectionTitle(split)
            Spacer(Modifier.height(6.dp))
            Text(
                state.splitRationale.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReviewStep(state: OnboardingUiState, onEditStep: (OnboardingStep) -> Unit) {
    Text(
        "Here is what we will build your programme from.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TwmCard(onClick = { onEditStep(OnboardingStep.BODY) }) {
        SectionTitle("Body", trailing = { Text("Edit", style = MaterialTheme.typography.labelLarge) })
        Spacer(Modifier.height(6.dp))
        DetailRow("Height / weight", "${state.heightCm} cm · ${state.weightKg} kg")
        state.metrics?.let { metrics ->
            DetailRow("BMI", "${metrics.bmi} (${metrics.bmiCategory.name.lowercase()})")
            DetailRow("Body fat", metrics.bodyFatPercent?.let { "$it%" } ?: "not provided")
            DetailRow("BMR / TDEE", "${metrics.bmrKcal} / ${metrics.tdeeKcal} kcal")
        }
    }
    TwmCard(onClick = { onEditStep(OnboardingStep.GOAL) }) {
        SectionTitle("Goal", trailing = { Text("Edit", style = MaterialTheme.typography.labelLarge) })
        Spacer(Modifier.height(6.dp))
        DetailRow("Primary goal", state.goal?.label ?: "Build muscle")
        state.nutrition?.let { nutrition ->
            DetailRow(
                "Daily targets",
                "${nutrition.calories} kcal · ${nutrition.proteinG}P / ${nutrition.fatG}F / ${nutrition.carbsG}C",
            )
        }
    }
    TwmCard(onClick = { onEditStep(OnboardingStep.SCHEDULE) }) {
        SectionTitle("Schedule", trailing = { Text("Edit", style = MaterialTheme.typography.labelLarge) })
        Spacer(Modifier.height(6.dp))
        DetailRow("Split", state.splitName ?: "Full body ×3")
        DetailRow("Frequency", "${state.daysPerWeek} days · ${state.sessionMinutes} min")
        DetailRow("Equipment", state.equipmentAccess?.label ?: "Full gym")
    }
    if (state.warnings.isNotEmpty()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MessageBanner(
            text = state.warnings.joinToString(" ") { warningText(it.code, it.args) },
            tone = BannerTone.WARNING,
        )
    }
    if (state.nutrition?.isCalorieFloorApplied == true) {
        MessageBanner(
            "Your calorie target was raised to a safe minimum for your body size.",
            tone = BannerTone.INFO,
        )
    }
    if (state.isEditingExistingProfile) {
        MessageBanner(
            "Saving updates your profile. Your current plan is kept until you rebuild it from the dashboard.",
            tone = BannerTone.INFO,
        )
    }
}

internal fun warningText(code: IssueCode, args: List<String>): String =
    when (code) {
        IssueCode.UNDERWEIGHT_WITH_FAT_LOSS ->
            "Your BMI is ${args.firstOrNull() ?: "low"} and your goal is fat loss. Building muscle is usually " +
                "the better first step at this weight — please consider professional guidance."
        IssueCode.MINOR_NEEDS_SUPERVISION ->
            "You are under 18: train with supervision and prioritise technique over load."
        IssueCode.VERY_HIGH_BODY_FAT ->
            "At this body-fat level a doctor's check-in before an aggressive deficit is wise."
        IssueCode.WAIST_TO_HEIGHT_RISK ->
            "Your waist-to-height ratio is ${args.firstOrNull() ?: "high"}; a slower deficit and more " +
                "walking are safer than a crash diet."
        IssueCode.AGGRESSIVE_RATE ->
            "That rate of change is aggressive. Aim for at most ${args.getOrNull(0) ?: "1"} kg/week loss " +
                "or ${args.getOrNull(1) ?: "0.4"} kg/week gain."
        IssueCode.DISCLAIMER_REQUIRED -> "Accept the disclaimer to continue."
        IssueCode.REQUIRED -> "This field is required."
        IssueCode.OUT_OF_RANGE ->
            "Value must be between ${args.getOrNull(0)} and ${args.getOrNull(1)}."
    }

@Composable
private fun TwmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        isError = isError,
        supportingText = if (isError && errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun OnboardingPreview() {
    TrainWithMeTheme(darkTheme = true) {
        Box(Modifier.fillMaxSize()) {
            OnboardingContent(
                state = OnboardingUiState(
                    stepIndex = OnboardingStep.REVIEW.ordinal,
                    name = "Ravi",
                    age = "26",
                    heightCm = "178",
                    weightKg = "78",
                    bodyFatPercent = "24",
                    experience = ExperienceLevel.BEGINNER,
                    activityLevel = ActivityLevel.LIGHTLY_ACTIVE,
                    goal = Goal.LOSE_FAT,
                    equipmentAccess = EquipmentAccess.FULL_GYM,
                    daysPerWeek = 3,
                    sessionMinutes = 60,
                    agreedToDisclaimer = true,
                ).withPreview(),
                onNameChange = {},
                onSexChange = {},
                onAgeChange = {},
                onHeightChange = {},
                onWeightChange = {},
                onBodyFatChange = {},
                onNeckChange = {},
                onWaistChange = {},
                onHipChange = {},
                onChestChange = {},
                onArmChange = {},
                onThighChange = {},
                onExperienceChange = {},
                onActivityChange = {},
                onGoalChange = {},
                onEquipmentChange = {},
                onDaysChange = {},
                onMinutesChange = {},
                onDisclaimerAccepted = {},
                onNext = {},
                onBack = {},
                onEditStep = {},
            )
        }
    }
}
