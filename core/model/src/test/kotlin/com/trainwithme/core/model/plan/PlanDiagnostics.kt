package com.trainwithme.core.model.plan

import com.trainwithme.core.model.Fixtures
import com.trainwithme.core.model.profile.EquipmentAccess
import com.trainwithme.core.model.profile.Goal
import java.time.LocalDate

/**
 * Not a test: a small report used while tuning the generator. It is excluded from CI by only
 * running when invoked explicitly (`./gradlew :core:model:test --tests *PlanDiagnostics*`).
 */
internal object PlanDiagnostics {

    fun report() {
        val generator = PlanGenerator(Fixtures.library, clock = { 0 })
        val start = LocalDate.of(2026, 11, 2)
        listOf(2, 3, 4, 5, 6).forEach { days ->
            val plan = (generator.generate(
                PlanGenerationRequest(Fixtures.profile(days = days), start),
            ) as PlanGenerationResult.Success).plan
            println("=== ${days} days — ${plan.splitName}")
            plan.week(1)!!.days.forEach { day ->
                if (day.isRestDay) return@forEach
                println(
                    "  ${day.title}: ${day.exercises.size} ex, ${day.totalSets} sets, " +
                        "${day.estimatedMinutes} min (budget ${plan.snapshot.sessionMinutes})",
                )
                day.exercises.forEach { e ->
                    println("      ${e.name} [${e.pattern}] ${e.prescription.display} rest ${e.prescription.restSeconds}s")
                }
            }
            val volume = PlanValidator.volume(plan.week(1)!!)
            println("  weekly volume: ${volume.perFamily.entries.sortedBy { it.key }}")
            val violations = PlanValidator.validate(plan, Fixtures.library, Fixtures.profile(days = days))
            println("  violations: $violations")
        }
        Goal.entries.forEach { goal ->
            val plan = (generator.generate(
                PlanGenerationRequest(Fixtures.profile(goal = goal, days = 4), start),
            ) as PlanGenerationResult.Success).plan
            val day = plan.trainingDays.first()
            println(
                "$goal -> ${plan.nutrition.calories} kcal / ${plan.nutrition.proteinG}P " +
                    "/ ${day.title}: ${day.exercises.first().prescription.display}",
            )
        }
        EquipmentAccess.entries.forEach { access ->
            val profile = Fixtures.profile(equipment = access, days = 3)
            val plan = (generator.generate(
                PlanGenerationRequest(profile, start),
            ) as PlanGenerationResult.Success).plan
            println(
                "$access -> week1 day2: " +
                    plan.week(0)!!.days[2].exercises.joinToString { it.name },
            )
        }
    }
}
