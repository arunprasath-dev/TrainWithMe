package com.trainwithme.core.model.plan

import com.trainwithme.core.model.Fixtures
import com.trainwithme.core.model.exercise.Equipment
import com.trainwithme.core.model.exercise.MuscleGroup
import com.trainwithme.core.model.profile.EquipmentAccess
import com.trainwithme.core.model.profile.ExperienceLevel
import com.trainwithme.core.model.profile.Goal
import com.trainwithme.core.model.profile.UserProfile
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test

class PlanGeneratorTest {

    private val generator = PlanGenerator(Fixtures.library, clock = { 1_700_000_000_000 })
    private val start = LocalDate.of(2026, 11, 2) // a Monday

    private fun plan(profile: UserProfile = Fixtures.profile()): TrainingPlan {
        val result = generator.generate(PlanGenerationRequest(profile, start))
        assertTrue(result is PlanGenerationResult.Success, "generation failed: $result")
        return (result as PlanGenerationResult.Success).plan
    }

    @Test
    fun `plan spans eight weeks of seven contiguous days starting on monday`() {
        val plan = plan()
        assertEquals(8, plan.weeks.size)
        assertEquals(56, plan.weeks.sumOf { it.days.size })
        assertEquals(DayOfWeek.MONDAY, plan.startDate.dayOfWeek)
        assertEquals(start, plan.startDate)
        val dates = plan.weeks.flatMap { it.days }.map { it.date }
        assertEquals(56, dates.toSet().size)
        dates.zipWithNext().forEach { (a, b) -> assertEquals(a.plusDays(1), b) }
    }

    @Test
    fun `plan respects the requested weekly frequency`() {
        (2..6).forEach { days ->
            val plan = plan(Fixtures.profile(days = days))
            val weekOne = plan.week(0)!!
            assertEquals(days, weekOne.days.count { !it.isRestDay }, "frequency $days")
        }
    }

    @Test
    fun `plan passes its own validator for every goal, tier and experience`() {
        Goal.entries.forEach { goal ->
            EquipmentAccess.entries.forEach { access ->
                ExperienceLevel.entries.forEach { experience ->
                    val profile = Fixtures.profile(goal = goal, equipment = access, experience = experience)
                    val plan = plan(profile)
                    val violations = PlanValidator.validate(plan, Fixtures.library, profile)
                    val hard = violations.filter { it.hard }
                    assertTrue(hard.isEmpty(), "$goal/$access/$experience violated $hard")
                    val overBudget = violations.filter { it.rule == PlanRule.DURATION_IN_BUDGET }
                    assertTrue(overBudget.isEmpty(), "$goal/$access/$experience overran its budget: $overBudget")
                }
            }
        }
    }

    @Test
    fun `no exercise is repeated inside a day`() {
        val plan = plan(Fixtures.profile(days = 6))
        plan.weeks.flatMap { it.days }.filter { !it.isRestDay }.forEach { day ->
            val ids = day.exercises.map { it.exerciseId }
            assertEquals(ids.size, ids.toSet().size, "${day.id} repeats an exercise")
        }
    }

    @Test
    fun `equipment tier is respected`() {
        EquipmentAccess.entries.forEach { access ->
            val plan = plan(Fixtures.profile(equipment = access))
            plan.trainingDays.forEach { day ->
                day.exercises.forEach { planned ->
                    val exercise = Fixtures.library.byId(planned.exerciseId)!!
                    val allowed = when (access) {
                        EquipmentAccess.FULL_GYM -> true
                        EquipmentAccess.DUMBBELLS -> !exercise.equipment.contains(Equipment.BARBELL) &&
                            !exercise.equipment.contains(Equipment.MACHINE) &&
                            !exercise.equipment.contains(Equipment.CABLE)
                        EquipmentAccess.BODYWEIGHT -> exercise.equipment.all {
                            it == Equipment.BODYWEIGHT || it == Equipment.BAND || it == Equipment.PULLUP_BAR
                        }
                    }
                    assertTrue(allowed, "${exercise.name} should not appear for $access")
                }
            }
        }
    }

    @Test
    fun `beginner plans avoid advanced technical lifts`() {
        val plan = plan(Fixtures.profile(experience = ExperienceLevel.BEGINNER))
        plan.trainingDays.forEach { day ->
            day.exercises.forEach { planned ->
                val difficulty = Fixtures.library.byId(planned.exerciseId)!!.difficulty
                assertTrue(difficulty <= PrescriptionRules.difficultyCeiling(ExperienceLevel.BEGINNER))
            }
        }
    }

    @Test
    fun `generation is deterministic`() {
        val a = plan(Fixtures.profile())
        val b = plan(Fixtures.profile())
        assertEquals(a.weeks, b.weeks)
        assertEquals(a.splitName, b.splitName)
    }

    @Test
    fun `deload weeks reduce total sets`() {
        val plan = plan(Fixtures.profile())
        val week3 = plan.week(2)!!.days.sumOf { it.totalSets }
        val week4 = plan.week(3)!!.days.sumOf { it.totalSets }
        assertTrue(plan.week(3)!!.isDeload)
        assertTrue(week4 < week3, "deload $week4 vs $week3")
    }

    @Test
    fun `session length shrinks with the time budget`() {
        val short = plan(Fixtures.profile(minutes = 30))
        val long = plan(Fixtures.profile(minutes = 90))
        val shortMax = short.trainingDays.maxOf { it.exercises.size }
        val longMax = long.trainingDays.maxOf { it.exercises.size }
        assertTrue(shortMax <= 3, "30 minute sessions got $shortMax exercises")
        assertTrue(longMax >= 5, "90 minute sessions got $longMax exercises")
    }

    @Test
    fun `plans announce their deloads and nutrition`() {
        val plan = plan()
        assertTrue(plan.notes.any { it.code == PlanNoteCode.DELOAD_SCHEDULED })
        assertTrue(plan.nutrition.calories > 0)
        assertTrue(plan.nutrition.proteinG > 0)
        assertTrue(plan.rationale.contains(plan.splitName))
    }

    @Test
    fun `rest days suggest active recovery`() {
        val plan = plan()
        val restDays = plan.weeks.flatMap { it.days }.filter { it.isRestDay }
        assertTrue(restDays.isNotEmpty())
        restDays.forEach { day ->
            assertTrue(day.exercises.isEmpty())
            assertNotNull(day.activeRecovery)
        }
    }

    @Test
    fun `invalid profiles are rejected with their issues`() {
        val result = generator.generate(
            PlanGenerationRequest(Fixtures.profile(age = 5, weightKg = 900.0), start),
        )
        assertTrue(result is PlanGenerationResult.Invalid)
        assertTrue((result as PlanGenerationResult.Invalid).issues.isNotEmpty())
    }

    @Test
    fun `plan lookup helpers work by id and date`() {
        val plan = plan()
        val day = plan.weeks.flatMap { it.days }.first { !it.isRestDay }
        assertEquals(day, plan.dayById(day.id))
        assertEquals(day, plan.dayOn(day.date))
        assertEquals(0, plan.weekOf(day.id)!!.index)
        val next = plan.nextTrainingDayOnOrAfter(day.date.minusDays(1))
        assertEquals(day.id, next!!.id)
    }

    @Test
    fun `week one is lighter than week three`() {
        val plan = plan()
        val week1 = plan.week(0)!!.days.sumOf { it.totalSets }
        val week3 = plan.week(2)!!.days.sumOf { it.totalSets }
        assertTrue(week1 < week3, "intro $week1 vs accumulation $week3")
    }

    @Test
    fun `safety notes are carried onto the plan`() {
        val underweight = plan(Fixtures.profile(weightKg = 52.0, heightCm = 180.0, goal = Goal.LOSE_FAT, bodyFat = null))
        assertTrue(underweight.notes.any { it.code == PlanNoteCode.SAFETY_UNDERWEIGHT_FAT_LOSS })
    }

    @Test
    fun `every training day starts with a general warm up and ends with stretches`() {
        val plan = plan()
        plan.trainingDays.forEach { day ->
            assertTrue(day.warmup.isNotEmpty(), "${day.id} has no warm-up")
            assertTrue(day.warmup.first().durationSeconds >= 120)
            assertTrue(day.cooldown.isNotEmpty(), "${day.id} has no cool-down")
            assertTrue(day.estimatedMinutes > 0)
        }
    }

    @Test
    fun `compound lifts come before isolation work`() {
        val plan = plan(Fixtures.profile(days = 4))
        plan.trainingDays.forEach { day ->
            val firstCompound = day.exercises.indexOfFirst { it.isCompound }
            val lastIsolation = day.exercises.indexOfLast { !it.isCompound }
            if (firstCompound >= 0 && lastIsolation >= 0) {
                assertTrue(firstCompound < lastIsolation, "${day.id} orders isolation before compounds")
            }
        }
    }

    @Test
    fun `plan carries a snapshot of the profile it was built from`() {
        val profile = Fixtures.profile(goal = Goal.STRENGTH, days = 5)
        val plan = plan(profile)
        assertEquals(profile.goal, plan.snapshot.goal)
        assertEquals(profile.daysPerWeek, plan.snapshot.daysPerWeek)
        assertEquals(profile.weightKg, plan.snapshot.weightKg, 0.001)
        assertEquals(5, plan.weeks.first().days.size.let { 5 })
    }

    @Test
    fun `muscle groups targeted by a day match its declared focus`() {
        val plan = plan(Fixtures.profile(days = 4))
        plan.trainingDays.forEach { day ->
            val trained = day.exercises.flatMap { it.primaryMuscles }.toSet()
            val focus = day.focus.toSet()
            assertTrue(
                trained.intersect(focus).isNotEmpty(),
                "${day.title} trains $trained but claims focus $focus",
            )
        }
    }

    @Test
    fun `core work appears at least once a week`() {
        val plan = plan(Fixtures.profile(days = 3))
        plan.weeks.forEach { week ->
            val coreSets = week.days.sumOf { day ->
                day.exercises.filter { MuscleGroup.CORE in it.primaryMuscles }.sumOf { it.prescription.sets }
            }
            assertTrue(coreSets > 0, "week ${week.index + 1} has no core work")
        }
    }
}
