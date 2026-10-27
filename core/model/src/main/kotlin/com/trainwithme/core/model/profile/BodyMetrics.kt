package com.trainwithme.core.model.profile

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/** Where a BMI falls, used for safety messaging (never for shaming copy). */
enum class BmiCategory { UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }

/**
 * Derived body-composition metrics. Pure functions of the profile + tape measurements.
 * All formulas are documented in docs/PLAN.md §7.1.
 */
data class BodyMetrics(
    val bmi: Double,
    val bmiCategory: BmiCategory,
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val leanMassKg: Double?,
    val fatMassKg: Double?,
    val ffmi: Double?,
    val bodyFatPercent: Double?,
    val bodyFatIsEstimated: Boolean,
    val waistToHeightRatio: Double?,
    val healthyWeightRangeKg: ClosedFloatingPointRange<Double>,
    val bmrMethod: BmrMethod,
) {
    enum class BmrMethod { MIFFLIN_ST_JEOR, KATCH_MCARDLE }
}

object BodyCompositionCalculator {

    /** Body-fat estimates outside this range are treated as bad input rather than as data. */
    private val PLAUSIBLE_BODY_FAT = 3.0..60.0

    fun compute(
        profile: UserProfile,
        circumference: Circumference = profile.circumferences,
    ): BodyMetrics {
        val heightM = profile.heightCm / 100.0
        val bmi = profile.weightKg / (heightM * heightM)

        val stated = profile.bodyFatPercent
        val estimated = if (stated == null && circumference.navyInputsAvailable(profile.sex)) {
            navyBodyFatPercent(profile.sex, profile.heightCm, circumference)
        } else {
            null
        }
        val bodyFat = (stated ?: estimated)?.takeIf { it in PLAUSIBLE_BODY_FAT }

        val fatMass = bodyFat?.let { profile.weightKg * it / 100.0 }
        val leanMass = fatMass?.let { profile.weightKg - it }

        val bmr = when {
            leanMass != null -> round(370 + 21.6 * leanMass).toInt()
            else -> round(mifflinStJeor(profile)).toInt()
        }
        val method = if (leanMass != null) {
            BodyMetrics.BmrMethod.KATCH_MCARDLE
        } else {
            BodyMetrics.BmrMethod.MIFFLIN_ST_JEOR
        }

        val ffmi = leanMass?.let { it / (heightM * heightM) }

        return BodyMetrics(
            bmi = round2(bmi),
            bmiCategory = bmiCategory(bmi),
            bmrKcal = bmr,
            tdeeKcal = round(bmr * profile.activityLevel.multiplier).toInt(),
            leanMassKg = leanMass?.let { round1(it) },
            fatMassKg = fatMass?.let { round1(it) },
            ffmi = ffmi?.let { round1(it) },
            bodyFatPercent = bodyFat?.let { round1(it) },
            bodyFatIsEstimated = stated == null && bodyFat != null,
            waistToHeightRatio = circumference.waistCm?.let { round2(it / profile.heightCm) },
            healthyWeightRangeKg = healthyWeightRange(profile.heightCm),
            bmrMethod = method,
        )
    }

    fun bmiCategory(bmi: Double): BmiCategory = when {
        bmi < 18.5 -> BmiCategory.UNDERWEIGHT
        bmi < 25.0 -> BmiCategory.NORMAL
        bmi < 30.0 -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }

    /** Mifflin-St Jeor: 10w + 6.25h - 5a + (5 male | -161 female). */
    fun mifflinStJeor(profile: UserProfile): Double {
        val sexTerm = if (profile.sex == Sex.MALE) 5.0 else -161.0
        return 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age + sexTerm
    }

    /**
     * US Navy circumference method. Returns null when the required measurements are missing or
     * the logarithms are undefined (which happens for non-physical input).
     */
    fun navyBodyFatPercent(sex: Sex, heightCm: Double, c: Circumference): Double? {
        val waist = c.waistCm ?: return null
        val neck = c.neckCm ?: return null
        return try {
            when (sex) {
                Sex.MALE -> {
                    val inner = waist - neck
                    if (inner <= 0) return null
                    495 / (1.0324 - 0.19077 * log10(inner) + 0.15456 * log10(heightCm)) - 450
                }
                Sex.FEMALE -> {
                    val hip = c.hipCm ?: return null
                    val inner = waist + hip - neck
                    if (inner <= 0) return null
                    495 / (1.29579 - 0.35004 * log10(inner) + 0.22100 * log10(heightCm)) - 450
                }
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Healthy weight = BMI 18.5..24.9 for the user's height. */
    fun healthyWeightRange(heightCm: Double): ClosedFloatingPointRange<Double> {
        val m2 = (heightCm / 100.0).pow(2)
        return round1(18.5 * m2)..round1(24.9 * m2)
    }

    /**
     * Protein target in g/kg is high for large bodies, so very high BMI users are dosed on lean
     * mass instead (see docs/PLAN.md §7.2 and risk R9).
     */
    fun proteinDoseKg(profile: UserProfile, metrics: BodyMetrics): Double {
        val lean = metrics.leanMassKg
        return if (metrics.bmi >= 30.0 && lean != null) {
            (lean * 2.4).coerceAtLeast(profile.weightKg * 1.6)
        } else {
            profile.weightKg
        }
    }

    private fun round1(v: Double): Double = round(v * 10) / 10
    private fun round2(v: Double): Double = round(v * 100) / 100
}
