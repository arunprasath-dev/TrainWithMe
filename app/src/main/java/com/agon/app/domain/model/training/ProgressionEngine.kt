package com.agon.app.domain.model.training

import com.agon.app.domain.model.exercise.Exercise
import com.agon.app.domain.model.exercise.MovementPattern
import com.agon.app.domain.model.plan.SetPrescription
import kotlin.math.round
import kotlin.math.roundToInt

/** A set the user actually performed (warm-ups excluded from progression maths). */
data class LoggedSet(
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
)

/** Why the engine suggests what it suggests — shown verbatim to the user. */
enum class SuggestionReason {
    FIRST_TIME,
    ADD_LOAD,
    ADD_REP,
    HOLD_LOAD,
    CONSOLIDATE,
    DELOAD,
}

data class LoadSuggestion(
    /** null = "no target yet", the user picks the starting weight. */
    val targetWeightKg: Double?,
    val targetReps: Int,
    val reason: SuggestionReason,
    val previousBest: LoggedSet?,
    val deltaKg: Double,
    val note: String?,
) {
    val hasHistory: Boolean get() = previousBest != null
}

/**
 * Double progression (docs/PLAN.md §7.5, Epic E):
 * 1. hit the top of the rep range on every working set at RPE ≤ 8.5 → add load, reset reps;
 * 2. otherwise add a rep;
 * 3. if the last session missed the bottom of the range, repeat the load and consolidate;
 * 4. deload weeks back off the load by ~15%.
 */
object ProgressionEngine {

    const val TOP_SET_RPE_CEILING = 8.5
    const val DELOAD_LOAD_FACTOR = 0.85

    fun suggest(
        exercise: Exercise,
        prescription: SetPrescription,
        previous: List<LoggedSet>,
        isDeloadWeek: Boolean = false,
    ): LoadSuggestion {
        val working = previous.filter { !it.isWarmup }
        if (working.isEmpty()) {
            return LoadSuggestion(
                targetWeightKg = null,
                targetReps = prescription.repLow,
                reason = SuggestionReason.FIRST_TIME,
                previousBest = null,
                deltaKg = 0.0,
                note = "First time on this movement — pick a weight that leaves 2–3 reps in reserve.",
            )
        }

        val best = working.maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))
        val topWeight = working.maxOf { it.weightKg }
        val increment = incrementKg(exercise)

        if (isDeloadWeek) {
            val deloadWeight = roundToIncrement(topWeight * DELOAD_LOAD_FACTOR, increment)
            return LoadSuggestion(
                targetWeightKg = deloadWeight,
                targetReps = prescription.repLow,
                reason = SuggestionReason.DELOAD,
                previousBest = best,
                deltaKg = round1(deloadWeight - topWeight),
                note = "Deload week: about 85% of your last load, stop 3–4 reps short of failure.",
            )
        }

        val allSetsAtTop = working.all { it.reps >= prescription.repHigh }
        val averageRpe = working.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()
        val rpeOk = averageRpe == null || averageRpe <= TOP_SET_RPE_CEILING

        if (allSetsAtTop && rpeOk) {
            val target = roundToIncrement(topWeight + increment, increment)
            return LoadSuggestion(
                targetWeightKg = target,
                targetReps = prescription.repLow,
                reason = SuggestionReason.ADD_LOAD,
                previousBest = best,
                deltaKg = round1(target - topWeight),
                note = "You hit ${prescription.repHigh} reps on every set — add " +
                    "${format(round1(target - topWeight))} kg and rebuild from ${prescription.repLow} reps.",
            )
        }

        if (allSetsAtTop && !rpeOk) {
            return LoadSuggestion(
                targetWeightKg = topWeight,
                targetReps = prescription.repHigh,
                reason = SuggestionReason.HOLD_LOAD,
                previousBest = best,
                deltaKg = 0.0,
                note = "You hit the top of the range but at high effort — keep this load until it feels " +
                    "like RPE 8, then add weight.",
            )
        }

        val allSetsAtBottom = working.all { it.reps >= prescription.repLow }
        if (allSetsAtBottom) {
            val nextReps = (working.minOf { it.reps } + 1).coerceAtMost(prescription.repHigh)
            return LoadSuggestion(
                targetWeightKg = topWeight,
                targetReps = nextReps,
                reason = SuggestionReason.ADD_REP,
                previousBest = best,
                deltaKg = 0.0,
                note = "Same load, one more rep per set (target $nextReps).",
            )
        }

        val missedBottom = working.any { it.reps < prescription.repLow }
        return LoadSuggestion(
            targetWeightKg = topWeight,
            targetReps = prescription.repLow,
            reason = if (missedBottom) SuggestionReason.CONSOLIDATE else SuggestionReason.HOLD_LOAD,
            previousBest = best,
            deltaKg = 0.0,
            note = if (missedBottom) {
                "You fell short last time — repeat this load and stay at RPE ≤ 8 until every set is clean."
            } else {
                "Repeat the load and add reps where you can."
            },
        )
    }

    /** Load increment by movement: heavy lower-body compounds move faster than small isolation. */
    fun incrementKg(exercise: Exercise): Double = when {
        !exercise.isCompound -> 1.0
        exercise.pattern in LOWER_BODY_PATTERNS -> 2.5
        else -> 1.25
    }

    /** Epley estimated one-rep max, used for the PR board. */
    fun epley(weightKg: Double, reps: Int): Double = when {
        reps <= 0 || weightKg <= 0 -> 0.0
        reps == 1 -> weightKg
        else -> weightKg * (1 + reps / 30.0)
    }

    fun totalVolumeKg(sets: List<LoggedSet>): Double =
        sets.filter { !it.isWarmup }.sumOf { it.weightKg * it.reps }

    fun roundToIncrement(value: Double, increment: Double): Double {
        if (increment <= 0) return round2(value)
        // Two decimals: 1.25 kg jumps must stay visible (61.25, not 61.2).
        return round2(round(value / increment) * increment)
    }

    private val LOWER_BODY_PATTERNS = setOf(
        MovementPattern.SQUAT,
        MovementPattern.HINGE,
        MovementPattern.LUNGE,
    )

    private fun round1(value: Double): Double = round(value * 10) / 10
    private fun round2(value: Double): Double = round(value * 100) / 100
    private fun format(value: Double): String = ((value * 100).roundToInt() / 100.0).toString()
}
