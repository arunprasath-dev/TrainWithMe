package com.trainwithme.feature.onboarding.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainwithme.core.data.repository.PlanRepository
import com.trainwithme.core.data.repository.ProfileRepository
import com.trainwithme.core.data.util.TimeProvider
import com.trainwithme.core.model.plan.PlanCalendar
import com.trainwithme.core.model.plan.PlanGenerationResult
import com.trainwithme.core.model.plan.SplitCatalog
import com.trainwithme.core.model.profile.ActivityLevel
import com.trainwithme.core.model.profile.BmiCategory
import com.trainwithme.core.model.profile.BodyCompositionCalculator
import com.trainwithme.core.model.profile.BodyMetrics
import com.trainwithme.core.model.profile.Circumference
import com.trainwithme.core.model.profile.EquipmentAccess
import com.trainwithme.core.model.profile.ExperienceLevel
import com.trainwithme.core.model.profile.Goal
import com.trainwithme.core.model.profile.IssueCode
import com.trainwithme.core.model.profile.IssueSeverity
import com.trainwithme.core.model.profile.NutritionCalculator
import com.trainwithme.core.model.profile.NutritionTargets
import com.trainwithme.core.model.profile.ProfileField
import com.trainwithme.core.model.profile.ProfileIssue
import com.trainwithme.core.model.profile.ProfileValidator
import com.trainwithme.core.model.profile.Sex
import com.trainwithme.core.model.profile.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The wizard steps. Kept explicit so back/forward behaviour is deterministic. */
enum class OnboardingStep(val title: String) {
    WELCOME("Welcome"),
    BODY("About you"),
    MEASUREMENTS("Measurements"),
    EXPERIENCE("Training history"),
    GOAL("Your goal"),
    SCHEDULE("Your week"),
    REVIEW("Review"),
    ;

    val isLast: Boolean get() = this == REVIEW

    companion object {
        val ordered = entries
        fun of(index: Int): OnboardingStep = ordered[index.coerceIn(0, ordered.lastIndex)]
    }
}

/**
 * Everything the wizard renders. Numeric fields are strings because they come from text input;
 * they are parsed once, in [assembledProfile], so validation and preview share one code path.
 */
data class OnboardingUiState(
    val stepIndex: Int = 0,
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val bodyFatPercent: String = "",
    val neckCm: String = "",
    val waistCm: String = "",
    val hipCm: String = "",
    val chestCm: String = "",
    val armCm: String = "",
    val thighCm: String = "",
    val experience: ExperienceLevel? = null,
    val activityLevel: ActivityLevel? = null,
    val goal: Goal? = null,
    val equipmentAccess: EquipmentAccess? = null,
    val daysPerWeek: Int = 4,
    val sessionMinutes: Int = 60,
    val agreedToDisclaimer: Boolean = false,
    val isEditingExistingProfile: Boolean = false,
    val isSaving: Boolean = false,
    val errors: Map<ProfileField, IssueCode> = emptyMap(),
    val warnings: List<ProfileIssue> = emptyList(),
    val metrics: BodyMetrics? = null,
    val nutrition: NutritionTargets? = null,
    val splitName: String? = null,
    val splitRationale: String? = null,
    val failureMessage: String? = null,
) {
    val step: OnboardingStep get() = OnboardingStep.of(stepIndex)
    val progress: Float get() = (stepIndex + 1) / OnboardingStep.ordered.size.toFloat()
    val canGoBack: Boolean get() = stepIndex > 0
    val isUnderweight: Boolean get() = metrics?.bmiCategory == BmiCategory.UNDERWEIGHT
}

sealed interface OnboardingEffect {
    data object Finished : OnboardingEffect
    data object Cancelled : OnboardingEffect
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val planRepository: PlanRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        // Editing an existing profile pre-fills the wizard instead of asking for everything again.
        viewModelScope.launch {
            profileRepository.getProfile()?.let { profile -> _uiState.update { it.fromProfile(profile) } }
        }
    }

    fun onNameChange(value: String) = update { it.copy(name = value, errors = it.errors - ProfileField.NAME) }
    fun onSexChange(value: Sex) = update { it.copy(sex = value) }
    fun onAgeChange(value: String) = update { it.copy(age = value.digitsOnly(3), errors = it.errors - ProfileField.AGE) }
    fun onHeightChange(value: String) = update { it.copy(heightCm = value.decimalOnly(), errors = it.errors - ProfileField.HEIGHT) }
    fun onWeightChange(value: String) = update { it.copy(weightKg = value.decimalOnly(), errors = it.errors - ProfileField.WEIGHT) }
    fun onBodyFatChange(value: String) = update { it.copy(bodyFatPercent = value.decimalOnly(), errors = it.errors - ProfileField.BODY_FAT) }
    fun onNeckChange(value: String) = update { it.copy(neckCm = value.decimalOnly()) }
    fun onWaistChange(value: String) = update { it.copy(waistCm = value.decimalOnly()) }
    fun onHipChange(value: String) = update { it.copy(hipCm = value.decimalOnly()) }
    fun onChestChange(value: String) = update { it.copy(chestCm = value.decimalOnly()) }
    fun onArmChange(value: String) = update { it.copy(armCm = value.decimalOnly()) }
    fun onThighChange(value: String) = update { it.copy(thighCm = value.decimalOnly()) }

    fun onExperienceChange(value: ExperienceLevel) = update { it.copy(experience = value) }
    fun onActivityChange(value: ActivityLevel) = update { it.copy(activityLevel = value) }
    fun onGoalChange(value: Goal) = update { it.copy(goal = value) }
    fun onEquipmentChange(value: EquipmentAccess) = update { it.copy(equipmentAccess = value) }
    fun onDaysChange(value: Int) = update { it.copy(daysPerWeek = value.coerceIn(UserProfile.MIN_DAYS, UserProfile.MAX_DAYS)) }
    fun onMinutesChange(value: Int) = update {
        it.copy(
            sessionMinutes = value.coerceIn(UserProfile.MIN_SESSION_MINUTES, UserProfile.MAX_SESSION_MINUTES),
        )
    }

    fun onDisclaimerAccepted(accepted: Boolean) = update {
        it.copy(agreedToDisclaimer = accepted, errors = it.errors - ProfileField.DISCLAIMER)
    }

    fun next() {
        val state = _uiState.value
        val validation = validateStep(state)
        if (validation.errors.isNotEmpty()) {
            update { it.copy(errors = validation.errors) }
            return
        }
        if (state.step.isLast) {
            finish()
        } else {
            update {
                it.copy(
                    stepIndex = it.stepIndex + 1,
                    errors = emptyMap(),
                    warnings = validation.warnings,
                ).withPreview()
            }
        }
    }

    fun back() {
        val state = _uiState.value
        if (state.canGoBack) {
            update { it.copy(stepIndex = it.stepIndex - 1, errors = emptyMap()).withPreview() }
        } else {
            viewModelScope.launch { _effects.send(OnboardingEffect.Cancelled) }
        }
    }

    /** Jump to a step from the review screen's "edit" links. */
    fun goToStep(step: OnboardingStep) = update { it.copy(stepIndex = step.ordinal).withPreview() }

    private fun finish() {
        val state = _uiState.value
        val profile = state.assembledProfile() ?: run {
            update { it.copy(failureMessage = "Please complete the required fields.") }
            return
        }
        val errors = ProfileValidator.errors(profile)
        if (errors.isNotEmpty()) {
            update { it.copy(errors = ProfileValidator.firstErrorByField(profile).mapValues { e -> e.value.code }) }
            return
        }
        update { it.copy(isSaving = true, failureMessage = null) }
        viewModelScope.launch {
            runCatching {
                profileRepository.saveProfile(profile)
                // Editing body composition must never silently rewrite the programme: the
                // dashboard offers "rebuild my plan" instead (docs/PLAN.md §6.7 G3).
                if (!state.isEditingExistingProfile) {
                    when (val result = planRepository.generatePlan(profile, PlanCalendar.weekStart(timeProvider.today()))) {
                        is PlanGenerationResult.Invalid ->
                            error("We could not build a plan from those numbers. Check your age, height and weight.")
                        is PlanGenerationResult.Success -> Unit
                    }
                }
            }.onSuccess {
                update { it.copy(isSaving = false) }
                _effects.send(OnboardingEffect.Finished)
            }.onFailure { throwable ->
                update {
                    it.copy(
                        isSaving = false,
                        failureMessage = throwable.message ?: "Something went wrong while saving your profile.",
                    )
                }
            }
        }
    }

    private fun validateStep(state: OnboardingUiState): Validation {
        val profile = state.assembledProfile() ?: return Validation(
            errors = mapOf(ProfileField.WEIGHT to IssueCode.REQUIRED),
        )
        val issues = ProfileValidator.validate(profile)
        val errorFields = issues.filter { it.severity == IssueSeverity.ERROR }
        val stepFields = when (state.step) {
            OnboardingStep.WELCOME -> setOf(ProfileField.DISCLAIMER)
            OnboardingStep.BODY -> setOf(
                ProfileField.NAME,
                ProfileField.AGE,
                ProfileField.HEIGHT,
                ProfileField.WEIGHT,
                ProfileField.BODY_FAT,
            )
            OnboardingStep.MEASUREMENTS -> setOf(ProfileField.NECK, ProfileField.WAIST, ProfileField.HIP)
            OnboardingStep.EXPERIENCE, OnboardingStep.GOAL -> emptySet()
            OnboardingStep.SCHEDULE -> setOf(ProfileField.DAYS_PER_WEEK, ProfileField.SESSION_MINUTES)
            OnboardingStep.REVIEW -> ProfileField.entries.toSet()
        }
        return Validation(
            errors = errorFields.filter { it.field in stepFields }.associate { it.field to it.code },
            warnings = issues.filter { it.severity == IssueSeverity.WARNING },
        )
    }

    private fun update(block: (OnboardingUiState) -> OnboardingUiState) = _uiState.update(block)

    private data class Validation(
        val errors: Map<ProfileField, IssueCode> = emptyMap(),
        val warnings: List<ProfileIssue> = emptyList(),
    )
}

/** Recomputes the derived preview (metrics, macros, split) after every change. */
internal fun OnboardingUiState.withPreview(): OnboardingUiState {
    val profile = assembledProfile() ?: return this.copy(metrics = null, nutrition = null, splitName = null)
    val metrics = BodyCompositionCalculator.compute(profile)
    val nutrition = NutritionCalculator.compute(profile, metrics)
    val split = SplitCatalog.forDaysPerWeek(profile.daysPerWeek)
    return copy(
        metrics = metrics,
        nutrition = nutrition,
        splitName = split.name,
        splitRationale = split.rationale,
        warnings = ProfileValidator.warnings(profile),
    )
}

/** Builds a profile from the text fields; null while a required number is still unparseable. */
internal fun OnboardingUiState.assembledProfile(): UserProfile? {
    val ageValue = age.toIntOrNull() ?: return null
    val height = heightCm.toDoubleOrNull() ?: return null
    val weight = weightKg.toDoubleOrNull() ?: return null
    return UserProfile(
        name = name.trim(),
        sex = sex,
        age = ageValue,
        heightCm = height,
        weightKg = weight,
        bodyFatPercent = bodyFatPercent.toDoubleOrNull(),
        experience = experience ?: ExperienceLevel.BEGINNER,
        activityLevel = activityLevel ?: ActivityLevel.LIGHTLY_ACTIVE,
        goal = goal ?: Goal.BUILD_MUSCLE,
        equipmentAccess = equipmentAccess ?: EquipmentAccess.FULL_GYM,
        daysPerWeek = daysPerWeek,
        sessionMinutes = sessionMinutes,
        circumferences = Circumference(
            neckCm = neckCm.toDoubleOrNull(),
            chestCm = chestCm.toDoubleOrNull(),
            waistCm = waistCm.toDoubleOrNull(),
            hipCm = hipCm.toDoubleOrNull(),
            armCm = armCm.toDoubleOrNull(),
            thighCm = thighCm.toDoubleOrNull(),
        ),
        agreedToDisclaimer = agreedToDisclaimer,
    )
}

internal fun OnboardingUiState.fromProfile(profile: UserProfile): OnboardingUiState = copy(
    name = profile.name,
    sex = profile.sex,
    age = profile.age.toString(),
    heightCm = profile.heightCm.trimZeros(),
    weightKg = profile.weightKg.trimZeros(),
    bodyFatPercent = profile.bodyFatPercent?.trimZeros().orEmpty(),
    neckCm = profile.circumferences.neckCm?.trimZeros().orEmpty(),
    waistCm = profile.circumferences.waistCm?.trimZeros().orEmpty(),
    hipCm = profile.circumferences.hipCm?.trimZeros().orEmpty(),
    chestCm = profile.circumferences.chestCm?.trimZeros().orEmpty(),
    armCm = profile.circumferences.armCm?.trimZeros().orEmpty(),
    thighCm = profile.circumferences.thighCm?.trimZeros().orEmpty(),
    experience = profile.experience,
    activityLevel = profile.activityLevel,
    goal = profile.goal,
    equipmentAccess = profile.equipmentAccess,
    daysPerWeek = profile.daysPerWeek,
    sessionMinutes = profile.sessionMinutes,
    agreedToDisclaimer = profile.agreedToDisclaimer,
    isEditingExistingProfile = true,
).withPreview()

private fun String.digitsOnly(maxLength: Int): String = filter { it.isDigit() }.take(maxLength)

private fun String.decimalOnly(): String {
    val filtered = filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
    val firstDot = filtered.indexOf('.')
    if (firstDot < 0) return filtered.take(6)
    return (filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).filter { it.isDigit() })
        .take(6)
}

private fun Double.trimZeros(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
