package com.agon.app.domain.model.plan

import com.agon.app.domain.model.exercise.MovementPattern
import com.agon.app.domain.model.profile.ExperienceLevel
import com.agon.app.domain.model.profile.Goal
import kotlin.math.roundToInt

/** How an exercise is used inside a session. Drives sets, reps, rest and tempo. */
enum class ExerciseRole { MAIN_COMPOUND, SECONDARY_COMPOUND, ISOLATION, CORE }

data class RepScheme(val low: Int, val high: Int, val restSeconds: Int)

/** One week of the mesocycle with its volume/intensity modifier (docs/PLAN.md §7.4). */
data class WeekModifier(
    val index: Int,
    val label: String,
    /** Percentage of the base set count for the week. */
    val volumePercent: Int,
    val targetRpe: Double,
    val isDeload: Boolean,
)

/**
 * All prescription maths for plan generation. Deliberately a pure object so the same rules can be
 * reasoned about in tests and in the docs table.
 */
object PrescriptionRules {

    val mesocycle: List<WeekModifier> = listOf(
        WeekModifier(0, "Introduction", 80, 7.0, false),
        WeekModifier(1, "Accumulation", 100, 7.5, false),
        WeekModifier(2, "Accumulation", 100, 8.0, false),
        WeekModifier(3, "Deload", 60, 6.0, true),
        WeekModifier(4, "Intensification", 100, 8.0, false),
        WeekModifier(5, "Intensification", 110, 8.5, false),
        WeekModifier(6, "Overreaching", 110, 9.0, false),
        WeekModifier(7, "Deload & retest", 60, 6.0, true),
    )

    fun weekModifier(weekIndex: Int): WeekModifier = mesocycle[weekIndex % mesocycle.size]

    fun roleFor(pattern: MovementPattern, isCompound: Boolean, priority: Int): ExerciseRole = when {
        pattern == MovementPattern.CORE -> ExerciseRole.CORE
        pattern == MovementPattern.CONDITIONING -> ExerciseRole.CORE
        isCompound && priority == 0 -> ExerciseRole.MAIN_COMPOUND
        isCompound -> ExerciseRole.SECONDARY_COMPOUND
        else -> ExerciseRole.ISOLATION
    }

    fun baseSets(role: ExerciseRole, experience: ExperienceLevel): Int = when (role) {
        ExerciseRole.MAIN_COMPOUND -> when (experience) {
            ExperienceLevel.BEGINNER -> 3
            ExperienceLevel.INTERMEDIATE -> 4
            ExperienceLevel.ADVANCED -> 5
        }
        ExerciseRole.SECONDARY_COMPOUND -> when (experience) {
            ExperienceLevel.BEGINNER -> 3
            ExperienceLevel.INTERMEDIATE -> 3
            ExperienceLevel.ADVANCED -> 4
        }
        ExerciseRole.ISOLATION -> when (experience) {
            ExperienceLevel.BEGINNER -> 2
            ExperienceLevel.INTERMEDIATE -> 3
            ExperienceLevel.ADVANCED -> 3
        }
        ExerciseRole.CORE -> 3
    }

    /** Goal-level set tweaks, applied after [baseSets]. */
    fun goalSetDelta(role: ExerciseRole, goal: Goal): Int = when (goal) {
        Goal.STRENGTH -> when (role) {
            ExerciseRole.MAIN_COMPOUND -> 1
            ExerciseRole.ISOLATION -> -1
            else -> 0
        }
        Goal.ENDURANCE -> if (role == ExerciseRole.ISOLATION) 1 else 0
        else -> 0
    }

    fun repScheme(role: ExerciseRole, goal: Goal, experience: ExperienceLevel): RepScheme =
        when (goal) {
            Goal.BUILD_MUSCLE -> when (role) {
                ExerciseRole.MAIN_COMPOUND -> RepScheme(5, 8, 150)
                ExerciseRole.SECONDARY_COMPOUND -> RepScheme(8, 12, 90)
                ExerciseRole.ISOLATION -> RepScheme(10, 15, 75)
                ExerciseRole.CORE -> RepScheme(12, 20, 45)
            }
            Goal.LOSE_FAT -> when (role) {
                ExerciseRole.MAIN_COMPOUND -> RepScheme(8, 12, 90)
                ExerciseRole.SECONDARY_COMPOUND -> RepScheme(10, 15, 60)
                ExerciseRole.ISOLATION -> RepScheme(12, 15, 45)
                ExerciseRole.CORE -> RepScheme(15, 20, 45)
            }
            Goal.RECOMP -> when (role) {
                ExerciseRole.MAIN_COMPOUND -> RepScheme(6, 10, 120)
                ExerciseRole.SECONDARY_COMPOUND -> RepScheme(8, 12, 90)
                ExerciseRole.ISOLATION -> RepScheme(12, 15, 60)
                ExerciseRole.CORE -> RepScheme(12, 20, 45)
            }
            Goal.STRENGTH -> when (role) {
                ExerciseRole.MAIN_COMPOUND -> if (experience == ExperienceLevel.BEGINNER) {
                    RepScheme(5, 8, 150)
                } else {
                    RepScheme(3, 5, 180)
                }
                ExerciseRole.SECONDARY_COMPOUND -> RepScheme(5, 8, 120)
                ExerciseRole.ISOLATION -> RepScheme(8, 12, 75)
                ExerciseRole.CORE -> RepScheme(8, 15, 60)
            }
            Goal.ENDURANCE -> when (role) {
                ExerciseRole.MAIN_COMPOUND -> RepScheme(12, 15, 60)
                ExerciseRole.SECONDARY_COMPOUND -> RepScheme(15, 20, 45)
                ExerciseRole.ISOLATION -> RepScheme(15, 20, 45)
                ExerciseRole.CORE -> RepScheme(15, 25, 40)
            }
        }

    fun tempo(role: ExerciseRole, goal: Goal): String = when {
        goal == Goal.STRENGTH && role == ExerciseRole.MAIN_COMPOUND -> "3-1-1"
        role == ExerciseRole.ISOLATION -> "2-1-1"
        role == ExerciseRole.CORE -> "2-0-1"
        else -> "2-0-1"
    }

    /** Final set count for a week, never below 2 working sets. */
    fun setsFor(
        role: ExerciseRole,
        experience: ExperienceLevel,
        goal: Goal,
        week: WeekModifier,
    ): Int {
        val base = baseSets(role, experience) + goalSetDelta(role, goal)
        val scaled = (base * week.volumePercent / 100.0).roundToInt()
        return scaled.coerceAtLeast(2)
    }

    fun prescription(
        role: ExerciseRole,
        experience: ExperienceLevel,
        goal: Goal,
        week: WeekModifier,
    ): SetPrescription {
        val scheme = repScheme(role, goal, experience)
        return SetPrescription(
            sets = setsFor(role, experience, goal, week),
            repLow = scheme.low,
            repHigh = scheme.high,
            restSeconds = scheme.restSeconds,
            rpeTarget = week.targetRpe,
            tempo = tempo(role, goal),
        )
    }

    /** Exercise slots that fit the user's time budget; priority 0–1 slots are never dropped. */
    fun exerciseCapacity(sessionMinutes: Int): Int = when {
        sessionMinutes <= 30 -> 3
        sessionMinutes <= 45 -> 4
        sessionMinutes <= 60 -> 5
        sessionMinutes <= 75 -> 6
        else -> 7
    }

    /** Conservative technical ceiling by experience. */
    fun difficultyCeiling(experience: ExperienceLevel): Int = when (experience) {
        ExperienceLevel.BEGINNER -> 3
        ExperienceLevel.INTERMEDIATE -> 4
        ExperienceLevel.ADVANCED -> 5
    }
}
