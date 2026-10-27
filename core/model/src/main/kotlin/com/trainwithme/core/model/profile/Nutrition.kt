package com.trainwithme.core.model.profile

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * Daily nutrition targets derived from body composition and goal. The app shows targets only —
 * food logging is out of scope for v1 (docs/PLAN.md §4.2).
 */
@Serializable
data class NutritionTargets(
    val calories: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
    val fiberG: Int,
    val waterMl: Int,
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val goal: Goal,
    val targetRateKgPerWeek: Double,
    val isCalorieFloorApplied: Boolean,
    val explanation: List<String>,
)

/** Result of the weekly adaptive calorie adjustment (docs/PLAN.md §7.2). */
data class NutritionAdjustment(
    val previousCalories: Int,
    val newCalories: Int,
    val deltaCalories: Int,
    val observedTrendKgPerWeek: Double,
    val reason: String,
)

object NutritionCalculator {

    /** Absolute safety floors — never prescribe below these without supervision. */
    const val MIN_CALORIES_FEMALE = 1_200
    const val MIN_CALORIES_MALE = 1_500
    private const val MAX_WEEKLY_ADJUSTMENT_KCAL = 250
    private const val KCAL_PER_KG_FAT = 7_700.0

    fun compute(
        profile: UserProfile,
        metrics: BodyMetrics = BodyCompositionCalculator.compute(profile),
    ): NutritionTargets {
        val goal = profile.goal
        val tdee = metrics.tdeeKcal.toDouble()
        val rate = profile.effectiveRateKgPerWeek

        val rawCalories = when (goal) {
            Goal.BUILD_MUSCLE -> tdee * 1.12
            Goal.LOSE_FAT -> tdee * 0.80
            Goal.RECOMP -> tdee
            Goal.STRENGTH -> tdee * 1.08
            Goal.ENDURANCE -> tdee
        }
        // Never eat below a small margin over measured BMR, and never below absolute floors.
        val bmrFloor = metrics.bmrKcal * 1.05
        val absoluteFloor = if (profile.sex == Sex.MALE) {
            MIN_CALORIES_MALE.toDouble()
        } else {
            MIN_CALORIES_FEMALE.toDouble()
        }
        val floor = maxOf(bmrFloor, absoluteFloor)
        val calories = rawCalories.coerceAtLeast(floor).roundToInt()
        val floorApplied = rawCalories < floor

        val proteinPerKg = when (goal) {
            Goal.BUILD_MUSCLE -> 1.8
            Goal.LOSE_FAT -> 2.2
            Goal.RECOMP -> 2.0
            Goal.STRENGTH -> 1.8
            Goal.ENDURANCE -> 1.6
        }
        val proteinBaseKg = BodyCompositionCalculator.proteinDoseKg(profile, metrics)
        var protein = (proteinBaseKg * proteinPerKg).roundToInt()

        var fatPerKg = when (goal) {
            Goal.BUILD_MUSCLE -> 0.9
            Goal.LOSE_FAT -> 0.8
            Goal.RECOMP -> 0.9
            Goal.STRENGTH -> 1.0
            Goal.ENDURANCE -> 0.9
        }
        var fat = (profile.weightKg * fatPerKg).roundToInt()

        fun carbsFor(p: Int, f: Int): Int =
            (((calories - p * 4 - f * 9).toDouble()) / 4.0).roundToInt().coerceAtLeast(0)

        var carbs = carbsFor(protein, fat)
        if (carbs == 0) {
            // Calorie budget is too tight for the prescribed protein+fat: shave fat first.
            fatPerKg = 0.6
            fat = (profile.weightKg * fatPerKg).roundToInt()
            carbs = carbsFor(protein, fat)
        }
        if (carbs == 0) {
            // Still impossible (very heavy user on a floor budget): reduce protein to 1.6 g/kg.
            protein = (proteinBaseKg * 1.6).roundToInt()
            carbs = carbsFor(protein, fat)
        }

        val fiber = (calories / 1000.0 * 14).roundToInt()
        val water = (profile.weightKg * 35).roundToInt()

        return NutritionTargets(
            calories = calories,
            proteinG = protein,
            fatG = fat,
            carbsG = carbs,
            fiberG = fiber,
            waterMl = water,
            bmrKcal = metrics.bmrKcal,
            tdeeKcal = metrics.tdeeKcal,
            goal = goal,
            targetRateKgPerWeek = rate,
            isCalorieFloorApplied = floorApplied,
            explanation = buildExplanation(profile, metrics, goal, calories, protein, fat, carbs, floorApplied),
        )
    }

    /**
     * Weekly adjustment once at least two weeks of weigh-ins exist. Moves calories toward the
     * goal rate by at most 250 kcal/week and never below the floor.
     */
    fun adjustWeekly(
        currentTargets: NutritionTargets,
        observedTrendKgPerWeek: Double,
        profile: UserProfile,
        metrics: BodyMetrics = BodyCompositionCalculator.compute(profile),
    ): NutritionAdjustment {
        val targetRate = currentTargets.targetRateKgPerWeek
        val isWeightGoal = currentTargets.goal == Goal.LOSE_FAT || currentTargets.goal == Goal.BUILD_MUSCLE
        val gap = targetRate - observedTrendKgPerWeek

        if (!isWeightGoal || kotlin.math.abs(gap) <= 0.15) {
            return NutritionAdjustment(
                previousCalories = currentTargets.calories,
                newCalories = currentTargets.calories,
                deltaCalories = 0,
                observedTrendKgPerWeek = observedTrendKgPerWeek,
                reason = if (isWeightGoal) {
                    "Trend is on target — targets unchanged."
                } else {
                    "Weight change is not the goal — targets unchanged."
                },
            )
        }

        val rawDelta = gap * KCAL_PER_KG_FAT / 7.0
        val delta = rawDelta.coerceIn(-MAX_WEEKLY_ADJUSTMENT_KCAL.toDouble(), MAX_WEEKLY_ADJUSTMENT_KCAL.toDouble())
        val floor = maxOf(
            metrics.bmrKcal * 1.05,
            if (profile.sex == Sex.MALE) MIN_CALORIES_MALE.toDouble() else MIN_CALORIES_FEMALE.toDouble(),
        )
        val newCalories = (currentTargets.calories + delta).coerceAtLeast(floor).roundToInt()
        val actualDelta = newCalories - currentTargets.calories
        val direction = if (actualDelta > 0) "increased" else if (actualDelta < 0) "reduced" else "held"
        return NutritionAdjustment(
            previousCalories = currentTargets.calories,
            newCalories = newCalories,
            deltaCalories = actualDelta,
            observedTrendKgPerWeek = observedTrendKgPerWeek,
            reason = "Your weight trend was ${formatSigned(observedTrendKgPerWeek)} kg/week against a " +
                "${formatSigned(targetRate)} kg/week target, so calories were $direction by " +
                "${kotlin.math.abs(actualDelta)} kcal.",
        )
    }

    private fun buildExplanation(
        profile: UserProfile,
        metrics: BodyMetrics,
        goal: Goal,
        calories: Int,
        protein: Int,
        fat: Int,
        carbs: Int,
        floorApplied: Boolean,
    ): List<String> = buildList {
        val method = if (metrics.bmrMethod == BodyMetrics.BmrMethod.KATCH_MCARDLE) {
            "Katch-McArdle (uses your lean mass)"
        } else {
            "Mifflin-St Jeor (age, height, weight, sex)"
        }
        add("BMR ${metrics.bmrKcal} kcal — $method.")
        add("TDEE ${metrics.tdeeKcal} kcal — BMR × ${profile.activityLevel.multiplier} for ${profile.activityLevel.label.lowercase()} activity.")
        val adjustment = when (goal) {
            Goal.BUILD_MUSCLE -> "+12% surplus to support muscle gain"
            Goal.LOSE_FAT -> "−20% deficit for fat loss"
            Goal.RECOMP -> "maintenance — recomp relies on high protein and heavy lifting"
            Goal.STRENGTH -> "+8% to fuel heavy sessions"
            Goal.ENDURANCE -> "maintenance to fuel training volume"
        }
        add("$calories kcal — TDEE $adjustment.")
        if (floorApplied) {
            add("Raised to the safety floor (${if (profile.sex == Sex.MALE) "1,500" else "1,200"} kcal minimum, and never below 105% of BMR).")
        }
        add("Protein ${protein} g — ${if (metrics.bmi >= 30.0) "dosed on lean mass" else "2 g/kg-class target for your goal"}.")
        add("Fat ${fat} g, carbs ${carbs} g — fat set first for hormones, carbs fill the remainder.")
        add("Fibre and water are secondary targets, not tracked meals.")
    }

    private fun formatSigned(value: Double): String {
        val rounded = (value * 100).roundToInt() / 100.0
        return if (rounded > 0) "+$rounded" else rounded.toString()
    }
}
