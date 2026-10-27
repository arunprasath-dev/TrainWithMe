package com.trainwithme.core.model.training

import com.trainwithme.core.model.Fixtures
import com.trainwithme.core.model.exercise.MovementPattern
import com.trainwithme.core.model.exercise.MuscleGroup
import com.trainwithme.core.model.exercise.Equipment
import com.trainwithme.core.model.plan.SetPrescription
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    private val lowerCompound = Fixtures.exercise(
        "squat-x",
        MovementPattern.SQUAT,
        listOf(MuscleGroup.QUADS),
        setOf(Equipment.BARBELL),
    )
    private val upperCompound = Fixtures.exercise(
        "bench-x",
        MovementPattern.HORIZONTAL_PUSH,
        listOf(MuscleGroup.CHEST),
        setOf(Equipment.BARBELL),
    )
    private val isolation = Fixtures.exercise(
        "curl-x",
        MovementPattern.BICEPS_ISOLATION,
        listOf(MuscleGroup.BICEPS),
        setOf(Equipment.DUMBBELL),
        compound = false,
    )
    private val prescription = SetPrescription(
        sets = 3,
        repLow = 8,
        repHigh = 12,
        restSeconds = 90,
        rpeTarget = 8.0,
        tempo = "2-0-1",
    )

    private fun sets(vararg reps: Int, weight: Double = 60.0, rpe: Double? = 8.0): List<LoggedSet> =
        reps.mapIndexed { index, rep -> LoggedSet(index, weight, rep, rpe) }

    @Test
    fun `first time on an exercise has no target load`() {
        val suggestion = ProgressionEngine.suggest(isolation, prescription, emptyList())
        assertEquals(SuggestionReason.FIRST_TIME, suggestion.reason)
        assertNull(suggestion.targetWeightKg)
        assertEquals(8, suggestion.targetReps)
    }

    @Test
    fun `hitting the top of the range with low rpe adds load`() {
        val suggestion = ProgressionEngine.suggest(lowerCompound, prescription, sets(12, 12, 12))
        assertEquals(SuggestionReason.ADD_LOAD, suggestion.reason)
        assertEquals(62.5, suggestion.targetWeightKg!!, 0.001)
        assertEquals(8, suggestion.targetReps)
        assertEquals(2.5, suggestion.deltaKg, 0.001)
    }

    @Test
    fun `upper body compounds progress in smaller increments`() {
        val suggestion = ProgressionEngine.suggest(upperCompound, prescription, sets(12, 12, 12))
        assertEquals(61.25, suggestion.targetWeightKg!!, 0.001)
    }

    @Test
    fun `isolation work progresses by the smallest increment`() {
        val suggestion = ProgressionEngine.suggest(isolation, prescription, sets(12, 12, 12))
        assertEquals(61.0, suggestion.targetWeightKg!!, 0.001)
    }

    @Test
    fun `top reps at high rpe holds the load`() {
        val suggestion = ProgressionEngine.suggest(lowerCompound, prescription, sets(12, 12, 12, rpe = 9.5))
        assertEquals(SuggestionReason.HOLD_LOAD, suggestion.reason)
        assertEquals(60.0, suggestion.targetWeightKg!!, 0.001)
    }

    @Test
    fun `inside the range adds a rep`() {
        val suggestion = ProgressionEngine.suggest(lowerCompound, prescription, sets(9, 8, 8))
        assertEquals(SuggestionReason.ADD_REP, suggestion.reason)
        assertEquals(60.0, suggestion.targetWeightKg!!, 0.001)
        assertEquals(9, suggestion.targetReps)
    }

    @Test
    fun `missing the bottom of the range consolidates`() {
        val suggestion = ProgressionEngine.suggest(lowerCompound, prescription, sets(6, 5, 5))
        assertEquals(SuggestionReason.CONSOLIDATE, suggestion.reason)
        assertEquals(60.0, suggestion.targetWeightKg!!, 0.001)
        assertTrue(suggestion.note!!.isNotBlank())
    }

    @Test
    fun `deload weeks back off the load`() {
        val suggestion = ProgressionEngine.suggest(lowerCompound, prescription, sets(12, 12, 12), isDeloadWeek = true)
        assertEquals(SuggestionReason.DELOAD, suggestion.reason)
        assertEquals(50.0, suggestion.targetWeightKg!!, 0.001)
    }

    @Test
    fun `warm up sets are ignored`() {
        val withWarmups = listOf(
            LoggedSet(0, 20.0, 10, isWarmup = true),
            LoggedSet(1, 40.0, 8, isWarmup = true),
        ) + sets(12, 12, 12)
        val suggestion = ProgressionEngine.suggest(lowerCompound, prescription, withWarmups)
        assertEquals(SuggestionReason.ADD_LOAD, suggestion.reason)
        assertEquals(62.5, suggestion.targetWeightKg!!, 0.001)
    }

    @Test
    fun `epley estimated max is monotonic`() {
        assertEquals(100.0, ProgressionEngine.epley(100.0, 1), 0.001)
        assertEquals(110.0, ProgressionEngine.epley(100.0, 3), 0.001)
        assertTrue(ProgressionEngine.epley(100.0, 10) > ProgressionEngine.epley(100.0, 5))
        assertEquals(0.0, ProgressionEngine.epley(0.0, 5), 0.001)
    }

    @Test
    fun `volume excludes warm up sets`() {
        val volume = ProgressionEngine.totalVolumeKg(
            listOf(
                LoggedSet(0, 100.0, 10, isWarmup = true),
                LoggedSet(1, 60.0, 10),
                LoggedSet(2, 60.0, 8),
            ),
        )
        assertEquals(1080.0, volume, 0.001)
    }

    @Test
    fun `rounding snaps to plate increments`() {
        assertEquals(62.5, ProgressionEngine.roundToIncrement(61.4, 2.5), 0.001)
        assertEquals(60.0, ProgressionEngine.roundToIncrement(58.9, 2.5), 0.001)
        assertEquals(61.0, ProgressionEngine.roundToIncrement(61.4, 1.0), 0.001)
    }
}
