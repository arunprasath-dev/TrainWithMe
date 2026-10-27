package com.trainwithme.core.model.profile

/** Fields the onboarding wizard validates, so the UI can attach an error to the right input. */
enum class ProfileField {
    NAME,
    AGE,
    HEIGHT,
    WEIGHT,
    BODY_FAT,
    DAYS_PER_WEEK,
    SESSION_MINUTES,
    WAIST,
    NECK,
    HIP,
    DISCLAIMER,
}

enum class IssueSeverity { ERROR, WARNING }

/** Stable codes; the UI maps these to localised strings (no English in the domain layer). */
enum class IssueCode {
    REQUIRED,
    OUT_OF_RANGE,
    UNDERWEIGHT_WITH_FAT_LOSS,
    MINOR_NEEDS_SUPERVISION,
    VERY_HIGH_BODY_FAT,
    WAIST_TO_HEIGHT_RISK,
    AGGRESSIVE_RATE,
    DISCLAIMER_REQUIRED,
}

data class ProfileIssue(
    val field: ProfileField,
    val severity: IssueSeverity,
    val code: IssueCode,
    val args: List<String> = emptyList(),
)

/**
 * Input validation + safety guardrails (docs/PLAN.md §6.1 A9, §15 R1).
 * Errors block plan generation; warnings are shown but do not block.
 */
object ProfileValidator {

    fun validate(profile: UserProfile): List<ProfileIssue> = buildList {
        if (profile.name.isBlank()) {
            add(ProfileIssue(ProfileField.NAME, IssueSeverity.ERROR, IssueCode.REQUIRED))
        }
        if (profile.age !in UserProfile.MIN_AGE..UserProfile.MAX_AGE) {
            add(rangeIssue(ProfileField.AGE, UserProfile.MIN_AGE, UserProfile.MAX_AGE))
        }
        if (profile.heightCm !in UserProfile.MIN_HEIGHT_CM..UserProfile.MAX_HEIGHT_CM) {
            add(rangeIssue(ProfileField.HEIGHT, UserProfile.MIN_HEIGHT_CM.toInt(), UserProfile.MAX_HEIGHT_CM.toInt()))
        }
        if (profile.weightKg !in UserProfile.MIN_WEIGHT_KG..UserProfile.MAX_WEIGHT_KG) {
            add(rangeIssue(ProfileField.WEIGHT, UserProfile.MIN_WEIGHT_KG.toInt(), UserProfile.MAX_WEIGHT_KG.toInt()))
        }
        profile.bodyFatPercent?.let { bf ->
            if (bf !in UserProfile.MIN_BODY_FAT..UserProfile.MAX_BODY_FAT) {
                add(rangeIssue(ProfileField.BODY_FAT, UserProfile.MIN_BODY_FAT.toInt(), UserProfile.MAX_BODY_FAT.toInt()))
            }
        }
        if (profile.daysPerWeek !in UserProfile.MIN_DAYS..UserProfile.MAX_DAYS) {
            add(rangeIssue(ProfileField.DAYS_PER_WEEK, UserProfile.MIN_DAYS, UserProfile.MAX_DAYS))
        }
        if (profile.sessionMinutes !in UserProfile.MIN_SESSION_MINUTES..UserProfile.MAX_SESSION_MINUTES) {
            add(
                rangeIssue(
                    ProfileField.SESSION_MINUTES,
                    UserProfile.MIN_SESSION_MINUTES,
                    UserProfile.MAX_SESSION_MINUTES,
                ),
            )
        }
        profile.circumferences.waistCm?.let { waist ->
            if (waist !in 40.0..200.0) add(rangeIssue(ProfileField.WAIST, 40, 200))
        }
        profile.circumferences.neckCm?.let { neck ->
            if (neck !in 20.0..70.0) add(rangeIssue(ProfileField.NECK, 20, 70))
        }
        profile.circumferences.hipCm?.let { hip ->
            if (hip !in 50.0..200.0) add(rangeIssue(ProfileField.HIP, 50, 200))
        }
        if (!profile.agreedToDisclaimer) {
            add(ProfileIssue(ProfileField.DISCLAIMER, IssueSeverity.ERROR, IssueCode.DISCLAIMER_REQUIRED))
        }

        // Safety guardrails — warnings only. The app never silently refuses to help.
        val metrics = BodyCompositionCalculator.compute(profile)
        if (metrics.bmiCategory == BmiCategory.UNDERWEIGHT && profile.goal == Goal.LOSE_FAT) {
            add(
                ProfileIssue(
                    ProfileField.WEIGHT,
                    IssueSeverity.WARNING,
                    IssueCode.UNDERWEIGHT_WITH_FAT_LOSS,
                    listOf(metrics.bmi.toString()),
                ),
            )
        }
        if (profile.age < 18) {
            add(ProfileIssue(ProfileField.AGE, IssueSeverity.WARNING, IssueCode.MINOR_NEEDS_SUPERVISION))
        }
        metrics.bodyFatPercent?.let { bf ->
            if (bf >= 40.0 && profile.goal == Goal.LOSE_FAT) {
                add(ProfileIssue(ProfileField.BODY_FAT, IssueSeverity.WARNING, IssueCode.VERY_HIGH_BODY_FAT))
            }
        }
        metrics.waistToHeightRatio?.let { ratio ->
            if (ratio >= 0.5) {
                add(
                    ProfileIssue(
                        ProfileField.WAIST,
                        IssueSeverity.WARNING,
                        IssueCode.WAIST_TO_HEIGHT_RISK,
                        listOf(ratio.toString()),
                    ),
                )
            }
        }
        val rate = profile.effectiveRateKgPerWeek
        val maxLoss = profile.weightKg * 0.01
        val maxGain = profile.weightKg * when (profile.experience) {
            ExperienceLevel.BEGINNER -> 0.005
            ExperienceLevel.INTERMEDIATE -> 0.0035
            ExperienceLevel.ADVANCED -> 0.0025
        }
        if (rate < -maxLoss || rate > maxGain) {
            add(
                ProfileIssue(
                    ProfileField.WEIGHT,
                    IssueSeverity.WARNING,
                    IssueCode.AGGRESSIVE_RATE,
                    listOf(String.format(java.util.Locale.US, "%.2f", maxLoss), String.format(java.util.Locale.US, "%.2f", maxGain)),
                ),
            )
        }
    }

    fun errors(profile: UserProfile): List<ProfileIssue> =
        validate(profile).filter { it.severity == IssueSeverity.ERROR }

    fun warnings(profile: UserProfile): List<ProfileIssue> =
        validate(profile).filter { it.severity == IssueSeverity.WARNING }

    fun isValid(profile: UserProfile): Boolean = errors(profile).isEmpty()

    /** Convenience for the wizard: the first issue per field, for inline errors. */
    fun firstErrorByField(profile: UserProfile): Map<ProfileField, ProfileIssue> =
        errors(profile).groupBy { it.field }.mapValues { it.value.first() }

    private fun rangeIssue(field: ProfileField, min: Int, max: Int): ProfileIssue =
        ProfileIssue(field, IssueSeverity.ERROR, IssueCode.OUT_OF_RANGE, listOf("$min", "$max"))
}
