package com.trainwithme.core.model.plan

import com.trainwithme.core.model.exercise.MovementPattern
import com.trainwithme.core.model.profile.ExperienceLevel
import com.trainwithme.core.model.profile.Goal
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class PrescriptionRulesTest {

    @Test
    fun `mesocycle has eight weeks with two deloads`() {
        assertEquals(8, PrescriptionRules.mesocycle.size)
        assertEquals(listOf(3, 7), PrescriptionRules.mesocycle.filter { it.isDeload }.map { it.index })
    }

    @Test
    fun `experience drives main compound sets`() {
        assertEquals(3, PrescriptionRules.baseSets(ExerciseRole.MAIN_COMPOUND, ExperienceLevel.BEGINNER))
        assertEquals(4, PrescriptionRules.baseSets(ExerciseRole.MAIN_COMPOUND, ExperienceLevel.INTERMEDIATE))
        assertEquals(5, PrescriptionRules.baseSets(ExerciseRole.MAIN_COMPOUND, ExperienceLevel.ADVANCED))
        assertEquals(2, PrescriptionRules.baseSets(ExerciseRole.ISOLATION, ExperienceLevel.BEGINNER))
    }

    @Test
    fun `strength goals add volume to main lifts and remove it from isolation`() {
        assertEquals(1, PrescriptionRules.goalSetDelta(ExerciseRole.MAIN_COMPOUND, Goal.STRENGTH))
        assertEquals(-1, PrescriptionRules.goalSetDelta(ExerciseRole.ISOLATION, Goal.STRENGTH))
        assertEquals(0, PrescriptionRules.goalSetDelta(ExerciseRole.MAIN_COMPOUND, Goal.BUILD_MUSCLE))
    }

    @Test
    fun `deload weeks reduce sets and rpe`() {
        val normal = PrescriptionRules.prescription(
            ExerciseRole.MAIN_COMPOUND,
            ExperienceLevel.INTERMEDIATE,
            Goal.BUILD_MUSCLE,
            PrescriptionRules.mesocycle[2],
        )
        val deload = PrescriptionRules.prescription(
            ExerciseRole.MAIN_COMPOUND,
            ExperienceLevel.INTERMEDIATE,
            Goal.BUILD_MUSCLE,
            PrescriptionRules.mesocycle[3],
        )
        assertTrue(deload.sets < normal.sets)
        assertTrue(deload.rpeTarget < normal.rpeTarget)
    }

    @Test
    fun `sets never fall below two`() {
        PrescriptionRules.mesocycle.forEach { week ->
            val sets = PrescriptionRules.setsFor(ExerciseRole.ISOLATION, ExperienceLevel.BEGINNER, Goal.STRENGTH, week)
            assertTrue(sets >= 2, "week ${week.index} produced $sets sets")
        }
    }

    @Test
    fun `rep schemes are ordered by goal`() {
        val strength = PrescriptionRules.repScheme(ExerciseRole.MAIN_COMPOUND, Goal.STRENGTH, ExperienceLevel.INTERMEDIATE)
        val hypertrophy = PrescriptionRules.repScheme(ExerciseRole.MAIN_COMPOUND, Goal.BUILD_MUSCLE, ExperienceLevel.INTERMEDIATE)
        val endurance = PrescriptionRules.repScheme(ExerciseRole.MAIN_COMPOUND, Goal.ENDURANCE, ExperienceLevel.INTERMEDIATE)
        assertTrue(strength.high < hypertrophy.high)
        assertTrue(hypertrophy.high < endurance.high)
        assertTrue(strength.restSeconds > hypertrophy.restSeconds)
    }

    @Test
    fun `beginners get a lower technical ceiling than advanced lifters`() {
        assertEquals(3, PrescriptionRules.difficultyCeiling(ExperienceLevel.BEGINNER))
        assertEquals(5, PrescriptionRules.difficultyCeiling(ExperienceLevel.ADVANCED))
    }

    @Test
    fun `time budget maps to a decreasing exercise count`() {
        val counts = listOf(30, 45, 60, 75, 90).map { PrescriptionRules.exerciseCapacity(it) }
        assertEquals(counts.sorted(), counts)
        assertEquals(3, counts.first())
        assertEquals(7, counts.last())
    }

    @Test
    fun `role assignment follows the slot`() {
        assertEquals(
            ExerciseRole.MAIN_COMPOUND,
            PrescriptionRules.roleFor(MovementPattern.SQUAT, isCompound = true, priority = 0),
        )
        assertEquals(
            ExerciseRole.SECONDARY_COMPOUND,
            PrescriptionRules.roleFor(MovementPattern.SQUAT, isCompound = true, priority = 2),
        )
        assertEquals(
            ExerciseRole.ISOLATION,
            PrescriptionRules.roleFor(MovementPattern.BICEPS_ISOLATION, isCompound = false, priority = 3),
        )
        assertEquals(
            ExerciseRole.CORE,
            PrescriptionRules.roleFor(MovementPattern.CORE, isCompound = false, priority = 4),
        )
    }

    @Test
    fun `prescription display is human readable`() {
        val rx = SetPrescription(sets = 4, repLow = 8, repHigh = 12, restSeconds = 90, rpeTarget = 8.0, tempo = "2-0-1")
        assertEquals("4 × 8–12", rx.display)
        assertEquals("4 × 10", rx.copy(repLow = 10, repHigh = 10).display)
    }
}
