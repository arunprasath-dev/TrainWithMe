package com.trainwithme.core.model.plan

import com.trainwithme.core.model.exercise.MovementPattern
import com.trainwithme.core.model.exercise.MuscleGroup
import com.trainwithme.core.model.profile.NutritionTargets
import com.trainwithme.core.model.profile.ProfileSnapshot
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** What a single set of an exercise should look like. */
@Serializable
data class SetPrescription(
    val sets: Int,
    val repLow: Int,
    val repHigh: Int,
    val restSeconds: Int,
    /** Target RPE for the working sets; 7 means ~3 reps in reserve. */
    val rpeTarget: Double,
    /** Tempo as ecc-pause-con (seconds), e.g. "2-0-1". */
    val tempo: String,
) {
    init {
        require(sets >= 1) { "A prescription needs at least one set" }
        require(repLow <= repHigh) { "Rep range must be ascending" }
        require(restSeconds >= 15) { "Rest below 15s is not a set prescription" }
    }

    val repLabel: String
        get() = if (repLow == repHigh) "$repLow" else "$repLow–$repHigh"

    val display: String
        get() = "$sets × $repLabel"

    /** RPE without a trailing `.0`, e.g. `8` instead of `8.0`. */
    val rpeLabel: String
        get() = if (rpeTarget % 1.0 == 0.0) rpeTarget.toInt().toString() else rpeTarget.toString()
}

/** One exercise inside a generated day. Numbers are final — deloads are already applied. */
@Serializable
data class PlannedExercise(
    val exerciseId: String,
    val name: String,
    val pattern: MovementPattern,
    val primaryMuscles: List<MuscleGroup>,
    val slotIndex: Int,
    val isCompound: Boolean,
    val prescription: SetPrescription,
    val videoClipId: String,
    val notes: String? = null,
) {
    /** Path handed to the player, relative to the APK's `assets/` root. */
    val videoAssetPath: String
        get() = "videos/$videoClipId.mp4"

    /** One-line prescription shown in list rows: `4 × 6–8 · rest 150s · RPE 8`. */
    val prescriptionSummary: String
        get() = "${prescription.display} · rest ${prescription.restSeconds}s · RPE ${prescription.rpeLabel}"
}

/** A warm-up checklist item. `durationSeconds == 0` means "for reps", shown in instructions. */
@Serializable
data class WarmupStep(
    val title: String,
    val instructions: String,
    val durationSeconds: Int,
)

@Serializable
data class StretchItem(
    val name: String,
    val targetMuscle: MuscleGroup,
    val durationSeconds: Int,
    val instructions: String,
)

@Serializable
data class PlanDay(
    val id: String,
    val weekIndex: Int,
    /** 0 = Monday … 6 = Sunday. */
    val dayOfWeek: Int,
    val dateIso: String,
    val title: String,
    val focus: List<MuscleGroup>,
    val isRestDay: Boolean,
    val estimatedMinutes: Int,
    val warmup: List<WarmupStep> = emptyList(),
    val exercises: List<PlannedExercise> = emptyList(),
    val cooldown: List<StretchItem> = emptyList(),
    val activeRecovery: String? = null,
) {
    val date: LocalDate get() = LocalDate.parse(dateIso)

    val totalSets: Int get() = exercises.sumOf { it.prescription.sets }

    val exerciseCount: Int get() = exercises.size

    val focusLabel: String get() = focus.distinct().joinToString(" · ") { it.label }
}

@Serializable
data class PlanWeek(
    val index: Int,
    val label: String,
    val isDeload: Boolean,
    val targetRpe: Double,
    val days: List<PlanDay>,
)

/** The generated programme. Immutable once activated (docs/PLAN.md §10). */
@Serializable
data class TrainingPlan(
    val id: String,
    val createdAtMillis: Long,
    val startDateIso: String,
    val splitName: String,
    val rationale: String,
    val weeks: List<PlanWeek>,
    val snapshot: ProfileSnapshot,
    val nutrition: NutritionTargets,
    /** Guardrail/selection notes produced at generation time; the UI owns the wording. */
    val notes: List<PlanNote> = emptyList(),
    val schemaVersion: Int = SCHEMA_VERSION,
) {
    init {
        require(weeks.isNotEmpty()) { "A plan needs at least one week" }
    }

    val startDate: LocalDate get() = LocalDate.parse(startDateIso)

    val weekCount: Int get() = weeks.size

    val trainingDays: List<PlanDay> get() = weeks.flatMap { it.days }.filter { !it.isRestDay }

    val totalPlannedSessions: Int get() = trainingDays.size

    fun week(index: Int): PlanWeek? = weeks.getOrNull(index)

    fun day(weekIndex: Int, dayOfWeek: Int): PlanDay? =
        weeks.getOrNull(weekIndex)?.days?.firstOrNull { it.dayOfWeek == dayOfWeek }

    fun dayById(dayId: String): PlanDay? = weeks.asSequence()
        .flatMap { it.days.asSequence() }
        .firstOrNull { it.id == dayId }

    fun weekOf(dayId: String): PlanWeek? = weeks.firstOrNull { week -> week.days.any { it.id == dayId } }

    /** The first training day on or after [date], wrapping to the start of the plan. */
    fun nextTrainingDayOnOrAfter(date: LocalDate): PlanDay? {
        val all = weeks.flatMap { it.days }
        return all.firstOrNull { !it.isRestDay && !it.date.isBefore(date) }
            ?: all.firstOrNull { !it.isRestDay }
    }

    fun dayOn(date: LocalDate): PlanDay? = weeks.asSequence()
        .flatMap { it.days.asSequence() }
        .firstOrNull { it.date == date }

    companion object {
        const val SCHEMA_VERSION = 1
        const val WEEKS_PER_MESOCYCLE = 8
        const val DAYS_PER_WEEK = 7
    }
}
