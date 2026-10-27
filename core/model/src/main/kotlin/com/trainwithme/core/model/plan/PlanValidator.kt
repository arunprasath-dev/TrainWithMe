package com.trainwithme.core.model.plan

import com.trainwithme.core.model.exercise.ExerciseLibrary
import com.trainwithme.core.model.exercise.VolumeFamily
import com.trainwithme.core.model.profile.ExperienceLevel
import com.trainwithme.core.model.profile.UserProfile

/** A rule the generator promises to keep. [hard] violations fail tests; soft ones are reported. */
data class PlanViolation(
    val rule: PlanRule,
    val hard: Boolean,
    val detail: String,
)

enum class PlanRule {
    WEEK_COUNT,
    DAY_COUNT,
    CONTIGUOUS_DATES,
    EXERCISE_EXISTS,
    EXERCISE_PERFORMABLE,
    NO_DUPLICATE_IN_DAY,
    MIN_EXERCISES,
    REST_DAY_EMPTY,
    PRESCRIPTION_SANE,
    DELOAD_REDUCES_VOLUME,
    VOLUME_IN_RANGE,
    DURATION_IN_BUDGET,
}

/** Weekly working-set totals per muscle family (primary muscles only). */
data class VolumeReport(val perFamily: Map<VolumeFamily, Int>) {
    val total: Int get() = perFamily.values.sum()
}

/**
 * Validates a generated plan against the guarantees in docs/PLAN.md §6.2. Used by unit tests and
 * by an assertion in debug builds after generation.
 */
object PlanValidator {

    data class VolumeBounds(val min: Int, val max: Int)

    /**
     * Effective weekly sets per muscle: primary muscles count 1.0 per set, secondary muscles 0.5
     * (indirect volume still drives growth). Deload weeks are exempt — they are *meant* to be low.
     * Upper bounds follow the literature's 10–20 hard sets plus indirect credit; lower bounds are
     * time-budget aware, because a 30-minute, 3-exercise session cannot deliver 20 sets.
     */
    fun volumeBounds(profile: UserProfile): VolumeBounds = when {
        profile.daysPerWeek <= 3 -> VolumeBounds(min = 4, max = 26)
        profile.experience == ExperienceLevel.BEGINNER -> VolumeBounds(min = 4, max = 30)
        else -> VolumeBounds(min = 4, max = 34)
    }

    /** Time tolerance: the estimate is intentionally approximate, UI labels it "about". */
    const val DURATION_TOLERANCE_MINUTES = 15

    fun validate(
        plan: TrainingPlan,
        library: ExerciseLibrary,
        profile: UserProfile? = null,
        expectedWeeks: Int = TrainingPlan.WEEKS_PER_MESOCYCLE,
    ): List<PlanViolation> = buildList {
        if (plan.weeks.size != expectedWeeks) {
            add(PlanViolation(PlanRule.WEEK_COUNT, true, "Expected $expectedWeeks weeks, got ${plan.weeks.size}"))
        }

        val allDays = plan.weeks.flatMap { it.days }
        if (allDays.size != expectedWeeks * TrainingPlan.DAYS_PER_WEEK) {
            add(PlanViolation(PlanRule.DAY_COUNT, true, "Expected ${expectedWeeks * 7} days, got ${allDays.size}"))
        }

        val sortedDates = allDays.map { it.date }.sorted()
        val contiguous = sortedDates.zipWithNext().all { (a, b) -> b == a.plusDays(1) }
        if (!contiguous) {
            add(PlanViolation(PlanRule.CONTIGUOUS_DATES, true, "Plan days are not contiguous"))
        }

        for (day in allDays) {
            if (day.isRestDay) {
                if (day.exercises.isNotEmpty()) {
                    add(PlanViolation(PlanRule.REST_DAY_EMPTY, true, "${day.id} is a rest day with exercises"))
                }
                continue
            }
            if (day.exercises.size < 2) {
                add(PlanViolation(PlanRule.MIN_EXERCISES, true, "${day.id} has only ${day.exercises.size} exercises"))
            }
            val ids = day.exercises.map { it.exerciseId }
            if (ids.size != ids.toSet().size) {
                add(PlanViolation(PlanRule.NO_DUPLICATE_IN_DAY, true, "${day.id} repeats an exercise"))
            }
            day.exercises.forEach { planned ->
                val exercise = library.byId(planned.exerciseId)
                if (exercise == null) {
                    add(PlanViolation(PlanRule.EXERCISE_EXISTS, true, "${planned.exerciseId} is not in the catalogue"))
                } else if (profile != null && !exercise.isPerformableWith(profile.equipmentAccess)) {
                    add(
                        PlanViolation(
                            PlanRule.EXERCISE_PERFORMABLE,
                            true,
                            "${exercise.name} needs ${exercise.equipment.joinToString { it.label }} " +
                                "which ${profile.equipmentAccess.label} does not include",
                        ),
                    )
                }
                val rx = planned.prescription
                if (rx.sets !in 1..8 || rx.repLow !in 1..40 || rx.repHigh !in rx.repLow..40 || rx.restSeconds !in 15..300) {
                    add(PlanViolation(PlanRule.PRESCRIPTION_SANE, true, "${planned.name} has an implausible prescription"))
                }
            }
            if (profile != null && day.estimatedMinutes > profile.sessionMinutes + DURATION_TOLERANCE_MINUTES) {
                add(
                    PlanViolation(
                        PlanRule.DURATION_IN_BUDGET,
                        false,
                        "${day.id} estimates ${day.estimatedMinutes} min for a ${profile.sessionMinutes} min budget",
                    ),
                )
            }
        }

        // Volume per week per family (deload weeks are intentionally below target).
        val bounds = profile?.let { volumeBounds(it) }
        plan.weeks.filter { !it.isDeload }.forEach { week ->
            val report = volume(week, library)
            report.perFamily.forEach { (family, sets) ->
                if (bounds != null && (sets < bounds.min || sets > bounds.max)) {
                    add(
                        PlanViolation(
                            PlanRule.VOLUME_IN_RANGE,
                            false,
                            "Week ${week.index + 1} $family volume $sets outside ${bounds.min}..${bounds.max}",
                        ),
                    )
                }
            }
        }

        // Deload weeks must actually reduce volume.
        plan.weeks.zipWithNext().forEach { (current, next) ->
            if (next.isDeload && !current.isDeload) {
                val currentVolume = current.days.sumOf { it.totalSets }
                val nextVolume = next.days.sumOf { it.totalSets }
                if (nextVolume >= currentVolume) {
                    add(
                        PlanViolation(
                            PlanRule.DELOAD_REDUCES_VOLUME,
                            true,
                            "Week ${next.index + 1} deload has $nextVolume sets vs $currentVolume the week before",
                        ),
                    )
                }
            }
        }
    }

    fun volume(week: PlanWeek, library: ExerciseLibrary? = null): VolumeReport {
        val totals = mutableMapOf<VolumeFamily, Double>()
        week.days.filter { !it.isRestDay }.forEach { day ->
            day.exercises.forEach { planned ->
                val sets = planned.prescription.sets.toDouble()
                planned.primaryMuscles.distinct().forEach { muscle ->
                    totals.merge(muscle.volumeFamily, sets, Double::plus)
                }
                val secondary = library?.byId(planned.exerciseId)?.secondaryMuscles.orEmpty()
                secondary.filter { it !in planned.primaryMuscles }
                    .map { it.volumeFamily }
                    .distinct()
                    .forEach { family -> totals.merge(family, sets / 2, Double::plus) }
            }
        }
        return VolumeReport(totals.mapValues { kotlin.math.round(it.value).toInt() })
    }

    fun isValid(
        plan: TrainingPlan,
        library: ExerciseLibrary,
        profile: UserProfile? = null,
    ): Boolean = validate(plan, library, profile).none { it.hard }
}
