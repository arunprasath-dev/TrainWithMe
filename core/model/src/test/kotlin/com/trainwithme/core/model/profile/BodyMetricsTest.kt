package com.trainwithme.core.model.profile

import com.trainwithme.core.model.Fixtures
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class BodyMetricsTest {

    @Test
    fun `bmi and category follow who cutoffs`() {
        val profile = Fixtures.profile(weightKg = 78.0, heightCm = 178.0)
        val metrics = BodyCompositionCalculator.compute(profile)
        assertEquals(24.62, metrics.bmi, 0.01)
        assertEquals(BmiCategory.NORMAL, metrics.bmiCategory)

        assertEquals(BmiCategory.UNDERWEIGHT, BodyCompositionCalculator.bmiCategory(18.4))
        assertEquals(BmiCategory.NORMAL, BodyCompositionCalculator.bmiCategory(24.9))
        assertEquals(BmiCategory.OVERWEIGHT, BodyCompositionCalculator.bmiCategory(25.0))
        assertEquals(BmiCategory.OBESE, BodyCompositionCalculator.bmiCategory(31.0))
    }

    @Test
    fun `mifflin st jeor matches reference values`() {
        // 10*78 + 6.25*178 - 5*28 + 5 = 1757.5 -> 1758
        val male = Fixtures.profile(weightKg = 78.0, heightCm = 178.0, age = 28, sex = Sex.MALE, bodyFat = null)
        assertEquals(1758, BodyCompositionCalculator.compute(male).bmrKcal)
        assertEquals(BodyMetrics.BmrMethod.MIFFLIN_ST_JEOR, BodyCompositionCalculator.compute(male).bmrMethod)

        // 10*62 + 6.25*165 - 5*31 - 161 = 1335.25 -> 1335
        val female = Fixtures.profile(weightKg = 62.0, heightCm = 165.0, age = 31, sex = Sex.FEMALE, bodyFat = null)
        assertEquals(1335, BodyCompositionCalculator.compute(female).bmrKcal)
    }

    @Test
    fun `katch mcardle is used when lean mass is known`() {
        val profile = Fixtures.profile(weightKg = 80.0, heightCm = 180.0, bodyFat = 20.0)
        val metrics = BodyCompositionCalculator.compute(profile)
        assertEquals(64.0, metrics.leanMassKg!!, 0.01)
        assertEquals(16.0, metrics.fatMassKg!!, 0.01)
        assertEquals((370 + 21.6 * 64).toInt(), metrics.bmrKcal)
        assertEquals(BodyMetrics.BmrMethod.KATCH_MCARDLE, metrics.bmrMethod)
        assertEquals(19.8, metrics.ffmi!!, 0.1)
    }

    @Test
    fun `tdee multiplies bmr by activity level`() {
        val profile = Fixtures.profile(bodyFat = null)
        val metrics = BodyCompositionCalculator.compute(profile)
        assertEquals(
            kotlin.math.round(metrics.bmrKcal * ActivityLevel.MODERATELY_ACTIVE.multiplier).toInt(),
            metrics.tdeeKcal,
        )
    }

    @Test
    fun `navy formula estimates body fat when only tape measurements exist`() {
        val circumferences = Circumference(neckCm = 38.0, waistCm = 90.0, hipCm = 98.0)
        val male = Fixtures.profile(bodyFat = null, circumferences = circumferences, sex = Sex.MALE)
        val metrics = BodyCompositionCalculator.compute(male)
        assertNotNull(metrics.bodyFatPercent)
        assertTrue(metrics.bodyFatPercent!! in 10.0..30.0, "estimated ${metrics.bodyFatPercent}")
        assertTrue(metrics.bodyFatIsEstimated)

        val female = Fixtures.profile(bodyFat = null, circumferences = circumferences, sex = Sex.FEMALE)
        assertNotNull(BodyCompositionCalculator.compute(female).bodyFatPercent)
    }

    @Test
    fun `stated body fat wins and is not flagged as estimated`() {
        val profile = Fixtures.profile(bodyFat = 18.0, circumferences = Circumference(neckCm = 38.0, waistCm = 90.0))
        val metrics = BodyCompositionCalculator.compute(profile)
        assertEquals(18.0, metrics.bodyFatPercent!!, 0.01)
        assertTrue(!metrics.bodyFatIsEstimated)
    }

    @Test
    fun `implausible body fat is discarded rather than used`() {
        val profile = Fixtures.profile(bodyFat = 80.0)
        val metrics = BodyCompositionCalculator.compute(profile)
        assertNull(metrics.bodyFatPercent)
        assertNull(metrics.leanMassKg)
    }

    @Test
    fun `healthy weight range brackets the user height`() {
        val range = BodyCompositionCalculator.healthyWeightRange(178.0)
        assertEquals(58.6, range.start, 0.2)
        assertEquals(78.9, range.endInclusive, 0.2)
    }

    @Test
    fun `waist to height ratio is computed when waist is present`() {
        val profile = Fixtures.profile(circumferences = Circumference(waistCm = 89.0))
        val metrics = BodyCompositionCalculator.compute(profile)
        assertEquals(0.5, metrics.waistToHeightRatio!!, 0.01)
    }

    @Test
    fun `protein is dosed on lean mass for very heavy bodies`() {
        val profile = Fixtures.profile(weightKg = 130.0, heightCm = 175.0, bodyFat = 40.0)
        val metrics = BodyCompositionCalculator.compute(profile)
        val dose = BodyCompositionCalculator.proteinDoseKg(profile, metrics)
        assertTrue(dose < profile.weightKg * 2.4, "dose $dose should be below the naive 2.4 g/kg target")
        assertTrue(dose >= profile.weightKg * 1.6)
    }
}
