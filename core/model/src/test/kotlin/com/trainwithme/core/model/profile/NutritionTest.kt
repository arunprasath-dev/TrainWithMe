package com.trainwithme.core.model.profile

import com.trainwithme.core.model.Fixtures
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class NutritionTest {

    @Test
    fun `fat loss runs a deficit and never dips below the floor`() {
        val profile = Fixtures.profile(goal = Goal.LOSE_FAT)
        val targets = NutritionCalculator.compute(profile)
        val metrics = BodyCompositionCalculator.compute(profile)
        assertTrue(targets.calories < metrics.tdeeKcal)
        assertTrue(targets.calories >= NutritionCalculator.MIN_CALORIES_MALE)
        assertEquals(-0.5, targets.targetRateKgPerWeek, 0.001)
    }

    @Test
    fun `muscle gain runs a surplus`() {
        val profile = Fixtures.profile(goal = Goal.BUILD_MUSCLE)
        val targets = NutritionCalculator.compute(profile)
        val metrics = BodyCompositionCalculator.compute(profile)
        assertTrue(targets.calories > metrics.tdeeKcal)
    }

    @Test
    fun `recomp and endurance sit at maintenance`() {
        val profile = Fixtures.profile(goal = Goal.RECOMP)
        val targets = NutritionCalculator.compute(profile)
        val metrics = BodyCompositionCalculator.compute(profile)
        assertEquals(metrics.tdeeKcal, targets.calories)
    }

    @Test
    fun `very small users are protected by the calorie floor`() {
        val profile = Fixtures.profile(
            goal = Goal.LOSE_FAT,
            sex = Sex.FEMALE,
            weightKg = 45.0,
            heightCm = 150.0,
            age = 55,
            bodyFat = null,
            rate = -1.5,
        )
        val targets = NutritionCalculator.compute(profile)
        assertTrue(targets.calories >= NutritionCalculator.MIN_CALORIES_FEMALE)
        assertTrue(targets.isCalorieFloorApplied)
    }

    @Test
    fun `macros add back up to the calorie target`() {
        Goal.entries.forEach { goal ->
            val targets = NutritionCalculator.compute(Fixtures.profile(goal = goal))
            val fromMacros = targets.proteinG * 4 + targets.fatG * 9 + targets.carbsG * 4
            assertTrue(
                kotlin.math.abs(fromMacros - targets.calories) <= 40,
                "$goal macros $fromMacros vs calories ${targets.calories}",
            )
            assertTrue(targets.carbsG >= 0)
            assertTrue(targets.fatG > 0)
        }
    }

    @Test
    fun `protein scales with body weight`() {
        val targets = NutritionCalculator.compute(Fixtures.profile(goal = Goal.LOSE_FAT, weightKg = 80.0, bodyFat = 20.0))
        assertTrue(targets.proteinG in 160..200, "protein ${targets.proteinG}")
    }

    @Test
    fun `fibre and water follow the published formulas`() {
        val targets = NutritionCalculator.compute(Fixtures.profile(weightKg = 80.0))
        assertEquals(kotlin.math.round(targets.calories / 1000.0 * 14).toInt(), targets.fiberG)
        assertEquals(2800, targets.waterMl)
        assertTrue(targets.fiberG > 0)
    }

    @Test
    fun `slower than target weight loss reduces calories`() {
        val profile = Fixtures.profile(goal = Goal.LOSE_FAT)
        val targets = NutritionCalculator.compute(profile)
        val adjustment = NutritionCalculator.adjustWeekly(
            currentTargets = targets,
            observedTrendKgPerWeek = -0.05,
            profile = profile,
        )
        assertTrue(adjustment.deltaCalories < 0, "expected a reduction, got ${adjustment.deltaCalories}")
        assertTrue(adjustment.deltaCalories >= -250)
    }

    @Test
    fun `faster than target weight loss increases calories`() {
        val profile = Fixtures.profile(goal = Goal.LOSE_FAT)
        val targets = NutritionCalculator.compute(profile)
        val adjustment = NutritionCalculator.adjustWeekly(targets, observedTrendKgPerWeek = -1.4, profile = profile)
        assertTrue(adjustment.deltaCalories > 0, "expected an increase, got ${adjustment.deltaCalories}")
        assertTrue(adjustment.deltaCalories <= 250)
    }

    @Test
    fun `on target trend leaves calories unchanged`() {
        val profile = Fixtures.profile(goal = Goal.LOSE_FAT)
        val targets = NutritionCalculator.compute(profile)
        val adjustment = NutritionCalculator.adjustWeekly(targets, observedTrendKgPerWeek = -0.45, profile = profile)
        assertEquals(0, adjustment.deltaCalories)
    }

    @Test
    fun `adjustment never drops below the floor`() {
        val profile = Fixtures.profile(goal = Goal.LOSE_FAT, sex = Sex.FEMALE, weightKg = 50.0, heightCm = 155.0, bodyFat = null, age = 45)
        val targets = NutritionCalculator.compute(profile)
        val adjustment = NutritionCalculator.adjustWeekly(targets.copy(calories = 1250), -1.2, profile)
        assertTrue(adjustment.newCalories >= NutritionCalculator.MIN_CALORIES_FEMALE)
    }

    @Test
    fun `strength goals do not adjust calories for weight trend`() {
        val profile = Fixtures.profile(goal = Goal.STRENGTH)
        val targets = NutritionCalculator.compute(profile)
        val adjustment = NutritionCalculator.adjustWeekly(targets, observedTrendKgPerWeek = 0.8, profile = profile)
        assertEquals(0, adjustment.deltaCalories)
    }

    @Test
    fun `explanation is generated for every goal`() {
        Goal.entries.forEach { goal ->
            val targets = NutritionCalculator.compute(Fixtures.profile(goal = goal))
            assertTrue(targets.explanation.size >= 4, "goal $goal explained in ${targets.explanation.size} lines")
        }
    }
}
