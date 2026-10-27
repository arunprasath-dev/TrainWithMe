package com.trainwithme.core.model.profile

import kotlinx.serialization.Serializable

/** Tape measurements in cm. All optional — used for trends and body-fat estimation. */
data class Circumference(
    val neckCm: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null,
) {
    val hasAny: Boolean
        get() = listOfNotNull(neckCm, chestCm, waistCm, hipCm, armCm, thighCm).isNotEmpty()

    /** US Navy formula is only valid when these are present. */
    fun navyInputsAvailable(sex: Sex): Boolean =
        when (sex) {
            Sex.MALE -> waistCm != null && neckCm != null
            Sex.FEMALE -> waistCm != null && neckCm != null && hipCm != null
        }

    companion object {
        val EMPTY = Circumference()
    }
}

/**
 * The single user of the app. Measurements are stored separately as time series so that this
 * object always represents the *current* snapshot used for planning.
 *
 * @param daysPerWeek training days per week (2..6).
 * @param sessionMinutes time budget per session (30..90).
 * @param goalRateKgPerWeek user-chosen rate of weight change; null = goal default.
 */
data class UserProfile(
    val id: Long = SINGLE_PROFILE_ID,
    val name: String,
    val sex: Sex,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val bodyFatPercent: Double? = null,
    val experience: ExperienceLevel,
    val activityLevel: ActivityLevel,
    val goal: Goal,
    val equipmentAccess: EquipmentAccess,
    val daysPerWeek: Int,
    val sessionMinutes: Int,
    val goalRateKgPerWeek: Double? = null,
    val circumferences: Circumference = Circumference.EMPTY,
    val agreedToDisclaimer: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val bodyFatFraction: Double?
        get() = bodyFatPercent?.let { it / 100.0 }

    val effectiveRateKgPerWeek: Double
        get() = goalRateKgPerWeek ?: goal.defaultRateKgPerWeek ?: 0.0

    companion object {
        const val SINGLE_PROFILE_ID = 1L
        const val MIN_AGE = 14
        const val MAX_AGE = 90
        const val MIN_HEIGHT_CM = 120.0
        const val MAX_HEIGHT_CM = 230.0
        const val MIN_WEIGHT_KG = 35.0
        const val MAX_WEIGHT_KG = 250.0
        const val MIN_BODY_FAT = 3.0
        const val MAX_BODY_FAT = 60.0
        const val MIN_DAYS = 2
        const val MAX_DAYS = 6
        const val MIN_SESSION_MINUTES = 30
        const val MAX_SESSION_MINUTES = 90
    }
}

/** Immutable copy of the inputs a plan was generated from, stored alongside the plan. */
@Serializable
data class ProfileSnapshot(
    val name: String,
    val sex: Sex,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val bodyFatPercent: Double?,
    val experience: ExperienceLevel,
    val activityLevel: ActivityLevel,
    val goal: Goal,
    val equipmentAccess: EquipmentAccess,
    val daysPerWeek: Int,
    val sessionMinutes: Int,
    val goalRateKgPerWeek: Double,
) {
    companion object {
        fun of(profile: UserProfile): ProfileSnapshot = ProfileSnapshot(
            name = profile.name,
            sex = profile.sex,
            age = profile.age,
            heightCm = profile.heightCm,
            weightKg = profile.weightKg,
            bodyFatPercent = profile.bodyFatPercent,
            experience = profile.experience,
            activityLevel = profile.activityLevel,
            goal = profile.goal,
            equipmentAccess = profile.equipmentAccess,
            daysPerWeek = profile.daysPerWeek,
            sessionMinutes = profile.sessionMinutes,
            goalRateKgPerWeek = profile.effectiveRateKgPerWeek,
        )
    }
}
