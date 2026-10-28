package com.agon.app.domain.model.plan

import com.agon.app.domain.model.exercise.Exercise
import com.agon.app.domain.model.exercise.ExerciseLibrary
import com.agon.app.domain.model.exercise.MuscleGroup
import com.agon.app.domain.model.profile.BodyCompositionCalculator
import com.agon.app.domain.model.profile.BodyMetrics
import com.agon.app.domain.model.profile.ExperienceLevel
import com.agon.app.domain.model.profile.Goal
import com.agon.app.domain.model.profile.IssueCode
import com.agon.app.domain.model.profile.NutritionCalculator
import com.agon.app.domain.model.profile.NutritionTargets
import com.agon.app.domain.model.profile.ProfileIssue
import com.agon.app.domain.model.profile.ProfileSnapshot
import com.agon.app.domain.model.profile.ProfileValidator
import com.agon.app.domain.model.profile.UserProfile
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToInt
import com.agon.app.domain.model.profile.EquipmentAccess

/** Inputs for one generation run. */
data class PlanGenerationRequest(
    val profile: UserProfile,
    val startDate: LocalDate,
    val weeks: Int = TrainingPlan.WEEKS_PER_MESOCYCLE,
    val seed: Int = 0,
    /** Optional stable id; the data layer passes a unique id, tests rely on the default. */
    val planId: String? = null,
)

sealed interface PlanGenerationResult {
    data class Success(val plan: TrainingPlan) : PlanGenerationResult
    data class Invalid(val issues: List<ProfileIssue>) : PlanGenerationResult
}

/** Calendar helpers so the UI and the engine agree on when week 1 starts. */
object PlanCalendar {
    /** The Monday of the week containing [date]; plans are always Monday-anchored. */
    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekStartOnOrAfter(date: LocalDate): LocalDate =
        if (date.dayOfWeek == DayOfWeek.MONDAY) date else date.with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    fun dateForWeekStart(weekStart: LocalDate, weekIndex: Int, dayOfWeek: Int): LocalDate =
        weekStart.plusDays(weekIndex * TrainingPlan.DAYS_PER_WEEK.toLong() + dayOfWeek)
}

/**
 * Turns a profile into an eight-week programme.
 *
 * Deterministic: the same profile, start date, seed and library always produce the same plan.
 * Selection rules are documented in docs/PLAN.md §7.3/§7.4; every invariant is asserted in
 * [PlanValidator] and covered by unit tests.
 */
class PlanGenerator(
    private val library: ExerciseLibrary,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun generate(request: PlanGenerationRequest): PlanGenerationResult {
        val profile = request.profile
        val errors = ProfileValidator.errors(profile)
        if (errors.isNotEmpty()) return PlanGenerationResult.Invalid(errors)

        val weekStart = PlanCalendar.weekStart(request.startDate)
        val split = SplitCatalog.forDaysPerWeek(profile.daysPerWeek)
        val metrics = BodyCompositionCalculator.compute(profile)
        val nutrition = NutritionCalculator.compute(profile, metrics)

        val notes = mutableListOf<PlanNote>()
        profileNotes(profile, metrics, notes)

        val weeks = (0 until request.weeks).map { weekIndex ->
            buildWeek(
                profile = profile,
                split = split,
                weekIndex = weekIndex,
                weekStart = weekStart,
                seed = request.seed,
                notes = notes,
            )
        }

        val plan = TrainingPlan(
            id = request.planId ?: "plan-${weekStart}-${request.seed}",
            createdAtMillis = clock(),
            startDateIso = weekStart.toString(),
            splitName = split.name,
            rationale = rationalFor(profile, split, nutrition),
            weeks = weeks,
            snapshot = ProfileSnapshot.of(profile),
            nutrition = nutrition,
            notes = notes.distinct(),
        )
        return PlanGenerationResult.Success(plan)
    }

    private fun buildWeek(
        profile: UserProfile,
        split: SplitDefinition,
        weekIndex: Int,
        weekStart: LocalDate,
        seed: Int,
        notes: MutableList<PlanNote>,
    ): PlanWeek {
        val modifier = PrescriptionRules.weekModifier(weekIndex)
        // Usage is tracked per week so no exercise is repeated unless the catalogue forces it.
        val weekUsage = mutableMapOf<String, Int>()
        val days = (0 until TrainingPlan.DAYS_PER_WEEK).map { dayOfWeek ->
            val date = PlanCalendar.dateForWeekStart(weekStart, weekIndex, dayOfWeek)
            val template = split.templateFor(dayOfWeek)
            if (template == null) {
                restDay(weekIndex, dayOfWeek, date)
            } else {
                buildTrainingDay(
                    profile = profile,
                    template = template,
                    modifier = modifier,
                    weekIndex = weekIndex,
                    dayOfWeek = dayOfWeek,
                    date = date,
                    seed = seed,
                    weekUsage = weekUsage,
                    notes = notes,
                )
            }
        }
        return PlanWeek(
            index = weekIndex,
            label = modifier.label,
            isDeload = modifier.isDeload,
            targetRpe = modifier.targetRpe,
            days = days,
        )
    }

    private fun buildTrainingDay(
        profile: UserProfile,
        template: DayTemplate,
        modifier: WeekModifier,
        weekIndex: Int,
        dayOfWeek: Int,
        date: LocalDate,
        seed: Int,
        weekUsage: MutableMap<String, Int>,
        notes: MutableList<PlanNote>,
    ): PlanDay {
        val capacity = PrescriptionRules.exerciseCapacity(profile.sessionMinutes)
        val usedToday = mutableSetOf<String>()
        val picks = mutableListOf<Pair<SlotSpec, Exercise>>()

        for ((slotIndex, slot) in template.slots.withIndex()) {
            val exercise = pickExercise(
                slot = slot,
                profile = profile,
                usedToday = usedToday,
                weekUsage = weekUsage,
                seed = seed,
                weekIndex = weekIndex,
                dayOfWeek = dayOfWeek,
                slotIndex = slotIndex,
            ) ?: continue
            usedToday += exercise.id
            picks += slot to exercise
        }

        // Rank the programme by slot priority (the main lifts first), then take as many as the
        // user's time budget allows. Display order follows the template, not the ranking.
        val ranked = picks.sortedWith(
            compareBy({ it.first.priority }, { template.slots.indexOf(it.first) }),
        )
        val kept = ranked.take(capacity)
        if (kept.size < picks.size) {
            notes += PlanNote(PlanNoteCode.SESSION_TRUNCATED_FOR_TIME)
        }

        val ordered = kept.sortedBy { template.slots.indexOf(it.first) }
        val planned = ordered.mapIndexed { index, (slot, exercise) ->
            val role = PrescriptionRules.roleFor(slot.pattern, exercise.isCompound, slot.priority)
            val prescription = PrescriptionRules.prescription(
                role = role,
                experience = profile.experience,
                goal = profile.goal,
                week = modifier,
            )
            plannedExercise(index, exercise, prescription, modifier)
        }
        val exercises = fitToTimeBudget(planned, profile, template)
        if (exercises.any { it.prescription.sets != planned.first { p -> p.exerciseId == it.exerciseId }.prescription.sets }) {
            notes += PlanNote(PlanNoteCode.SESSION_TRUNCATED_FOR_TIME)
        }
        exercises.forEach { weekUsage.merge(it.exerciseId, it.prescription.sets, Int::plus) }

        if (exercises.isEmpty()) {
            notes += PlanNote(PlanNoteCode.EQUIPMENT_LIMITED_SELECTION, listOf(profile.equipmentAccess.label))
        }

        val warmup = warmupFor(template, exercises.firstOrNull())
        val cooldown = cooldownFor(template.focus)
        return PlanDay(
            id = dayId(weekIndex, dayOfWeek),
            weekIndex = weekIndex,
            dayOfWeek = dayOfWeek,
            dateIso = date.toString(),
            title = template.title,
            focus = template.focus,
            isRestDay = false,
            estimatedMinutes = estimateMinutes(warmup, exercises, cooldown),
            warmup = warmup,
            exercises = exercises,
            cooldown = cooldown,
        )
    }

    private fun restDay(weekIndex: Int, dayOfWeek: Int, date: LocalDate): PlanDay = PlanDay(
        id = dayId(weekIndex, dayOfWeek),
        weekIndex = weekIndex,
        dayOfWeek = dayOfWeek,
        dateIso = date.toString(),
        title = "Rest & recover",
        focus = emptyList(),
        isRestDay = true,
        estimatedMinutes = 0,
        activeRecovery = "20–30 min easy walk or 10 min mobility flow. Aim for 7–9 hours of sleep — " +
            "that is where the adaptation happens.",
    )

    private fun plannedExercise(
        index: Int,
        exercise: Exercise,
        prescription: SetPrescription,
        modifier: WeekModifier,
    ): PlannedExercise = PlannedExercise(
        exerciseId = exercise.id,
        name = exercise.name,
        pattern = exercise.pattern,
        primaryMuscles = exercise.primaryMuscles,
        slotIndex = index,
        isCompound = exercise.isCompound,
        prescription = prescription,
        videoClipId = exercise.videoClipId,
        notes = if (modifier.isDeload) {
            "Deload: stop 3–4 reps short of failure, technique over load."
        } else {
            null
        },
    )

    /**
     * A session that does not fit the user's time budget is not a session they will finish.
     * Sets are trimmed (never below two) until the estimated work fits the available minutes.
     */
    private fun fitToTimeBudget(
        exercises: List<PlannedExercise>,
        profile: UserProfile,
        template: DayTemplate,
    ): List<PlannedExercise> {
        if (exercises.isEmpty()) return exercises
        val fixedSeconds = warmupSeconds(template) + cooldownSeconds(template.focus)
        val workBudget = profile.sessionMinutes * 60 - fixedSeconds
        val sets = exercises.map { it.prescription.sets }.toMutableList()

        fun workSeconds(counts: List<Int>): Int = exercises.indices.sumOf { i ->
            val rx = exercises[i].prescription
            counts[i] * (secondsPerSet(rx.repHigh) + rx.restSeconds) + TRANSITION_SECONDS
        }

        var guard = 0
        while (workSeconds(sets) > workBudget && sets.any { it > MIN_SETS } && guard < 256) {
            guard++
            val index = sets.indices.filter { sets[it] > MIN_SETS }.maxByOrNull { sets[it] } ?: break
            sets[index] = sets[index] - 1
        }

        return exercises.mapIndexed { i, exercise ->
            if (sets[i] == exercise.prescription.sets) {
                exercise
            } else {
                exercise.copy(prescription = exercise.prescription.copy(sets = sets[i]))
            }
        }
    }

    private fun secondsPerSet(repHigh: Int): Int = when {
        repHigh <= 6 -> 50
        repHigh <= 12 -> 40
        else -> 35
    }

    private fun warmupSeconds(template: DayTemplate): Int =
        GENERAL_WARMUP_SECONDS + template.warmupDrills.size * DRILL_SECONDS + RAMP_UP_SECONDS

    private fun cooldownSeconds(focus: List<MuscleGroup>): Int =
        focus.distinct().take(4).sumOf { STRETCHES[it]?.durationSeconds ?: 0 }

    /**
     * Ranking: least-used-this-week first, then pattern affinity, then compound/role fit, then
     * closeness to the athlete's level, then a stable hash for tie-breaking.
     */
    private fun pickExercise(
        slot: SlotSpec,
        profile: UserProfile,
        usedToday: Set<String>,
        weekUsage: Map<String, Int>,
        seed: Int,
        weekIndex: Int,
        dayOfWeek: Int,
        slotIndex: Int,
    ): Exercise? {
        val pool = library.all().filter { exercise ->
            exercise.isPerformableWith(profile.equipmentAccess) &&
                exercise.difficulty <= PrescriptionRules.difficultyCeiling(profile.experience) &&
                exercise.id !in usedToday
        }
        if (pool.isEmpty()) return null

        val patterns = listOf(slot.pattern) + slot.alternates
        for ((rank, pattern) in patterns.withIndex()) {
            val candidates = pool.filter { it.pattern == pattern }
            if (candidates.isNotEmpty()) {
                return candidates.minWith(comparator(profile, slot, weekUsage, seed, weekIndex, dayOfWeek, slotIndex, rank))
            }
        }

        // Last resort: any exercise that trains the slot's primary muscles, whatever the pattern.
        val fallback = pool.filter { ex -> ex.primaryMuscles.any { it in slot.primaryMuscles } }
        if (fallback.isNotEmpty()) {
            return fallback.minWith(
                comparator(profile, slot, weekUsage, seed, weekIndex, dayOfWeek, slotIndex, patterns.size),
            )
        }
        return null
    }

    private fun comparator(
        profile: UserProfile,
        slot: SlotSpec,
        weekUsage: Map<String, Int>,
        seed: Int,
        weekIndex: Int,
        dayOfWeek: Int,
        slotIndex: Int,
        patternRank: Int,
    ): Comparator<Exercise> {
        val targetDifficulty = when (profile.experience) {
            ExperienceLevel.BEGINNER -> 2
            ExperienceLevel.INTERMEDIATE -> 3
            ExperienceLevel.ADVANCED -> 4
        }
        return compareBy<Exercise>(
            { weekUsage[it.id] ?: 0 },
            { if (it.isCompound == slot.isCompound) 0 else 1 },
            { patternRank },
            { abs(it.difficulty - targetDifficulty) },
            { stableHash("$seed:$weekIndex:$dayOfWeek:$slotIndex:${it.id}") },
            { it.id },
        )
    }

    private fun warmupFor(template: DayTemplate, first: PlannedExercise?): List<WarmupStep> = buildList {
        add(
            WarmupStep(
                title = "General warm-up",
                instructions = "4–5 minutes easy cardio — bike, rower or brisk incline walk. Warm, not tired.",
                durationSeconds = GENERAL_WARMUP_SECONDS,
            ),
        )
        template.warmupDrills.forEach { drill ->
            add(
                WarmupStep(
                    title = drill,
                    instructions = "2 × 10 slow, controlled reps. Pause where it feels tight.",
                    durationSeconds = DRILL_SECONDS,
                ),
            )
        }
        first?.let {
            add(
                WarmupStep(
                    title = "Ramp-up sets — ${it.name}",
                    instructions = "1×8 at ~40%, 1×5 at ~60%, 1×3 at ~80% of your first working weight.",
                    durationSeconds = RAMP_UP_SECONDS,
                ),
            )
        }
    }

    private fun cooldownFor(focus: List<MuscleGroup>): List<StretchItem> =
        focus.distinct().take(4).mapNotNull { STRETCHES[it] }

    private fun estimateMinutes(
        warmup: List<WarmupStep>,
        exercises: List<PlannedExercise>,
        cooldown: List<StretchItem>,
    ): Int {
        val warmupSeconds = warmup.sumOf { it.durationSeconds }
        val workSeconds = exercises.sumOf { exercise ->
            exercise.prescription.sets * (secondsPerSet(exercise.prescription.repHigh) + exercise.prescription.restSeconds)
        }
        val transitions = exercises.size * TRANSITION_SECONDS
        val cooldownSeconds = cooldown.sumOf { it.durationSeconds }
        return ((warmupSeconds + workSeconds + transitions + cooldownSeconds) / 60.0).roundToInt()
    }

    private fun profileNotes(
        profile: UserProfile,
        metrics: BodyMetrics,
        notes: MutableList<PlanNote>,
    ) {
        ProfileValidator.warnings(profile).forEach { issue ->
            when (issue.code) {
                IssueCode.UNDERWEIGHT_WITH_FAT_LOSS ->
                    notes += PlanNote(PlanNoteCode.SAFETY_UNDERWEIGHT_FAT_LOSS)
                IssueCode.MINOR_NEEDS_SUPERVISION -> notes += PlanNote(PlanNoteCode.SAFETY_MINOR)
                IssueCode.AGGRESSIVE_RATE -> notes += PlanNote(PlanNoteCode.AGGRESSIVE_RATE)
                IssueCode.VERY_HIGH_BODY_FAT -> notes += PlanNote(PlanNoteCode.HIGH_BODY_FAT)
                IssueCode.WAIST_TO_HEIGHT_RISK -> notes += PlanNote(PlanNoteCode.WAIST_RISK)
                else -> Unit
            }
        }
        notes += PlanNote(PlanNoteCode.DELOAD_SCHEDULED, listOf("4", "8"))
        metrics.waistToHeightRatio?.let { ratio ->
            if (ratio < 0.5) notes.removeAll { it.code == PlanNoteCode.WAIST_RISK }
        }
    }

    private fun rationalFor(
        profile: UserProfile,
        split: SplitDefinition,
        nutrition: NutritionTargets,
    ): String {
        val goalLine = when (profile.goal) {
            Goal.BUILD_MUSCLE -> "Rep ranges of 5–12 with a small surplus build muscle without unnecessary fat gain."
            Goal.LOSE_FAT -> "Moderate reps, short rests and a 20% deficit preserve muscle while you lean out."
            Goal.RECOMP -> "Maintenance calories plus heavy, progressive work let you add muscle and drop fat slowly."
            Goal.STRENGTH -> "Low-rep main lifts with long rests build force production; isolation work fills the gaps."
            Goal.ENDURANCE -> "Higher reps and short rests build work capacity alongside your conditioning."
        }
        val equipmentLine = when (profile.equipmentAccess) {
            EquipmentAccess.FULL_GYM ->
                "Every exercise uses equipment in a full gym, ordered compounds first."
            EquipmentAccess.DUMBBELLS ->
                "Only dumbbell, bench, band and pull-up-bar movements are included."
            EquipmentAccess.BODYWEIGHT ->
                "Everything is bodyweight, band or pull-up-bar based."
        }
        val volumeLine = "${split.trainingDayIndices.size} sessions a week at about " +
            "${profile.sessionMinutes} minutes, ${nutrition.calories} kcal and ${nutrition.proteinG} g protein a day."
        return "${split.name} — ${split.rationale} $goalLine $equipmentLine $volumeLine"
    }

    private fun dayId(weekIndex: Int, dayOfWeek: Int): String = "w${weekIndex + 1}d${dayOfWeek + 1}"

    private fun stableHash(value: String): Int {
        var hash = 2_166_136_261L
        value.forEach { char ->
            hash = hash xor char.code.toLong()
            hash = (hash * 16_777_619L) and 0xFFFF_FFFFL
        }
        return (hash % Int.MAX_VALUE).toInt()
    }

    private companion object {
        const val GENERAL_WARMUP_SECONDS = 240
        const val DRILL_SECONDS = 45
        const val RAMP_UP_SECONDS = 120
        const val TRANSITION_SECONDS = 45
        const val MIN_SETS = 2

        val STRETCHES: Map<MuscleGroup, StretchItem> = mapOf(
            MuscleGroup.CHEST to StretchItem(
                "Doorway chest stretch",
                MuscleGroup.CHEST,
                30,
                "Forearm on the frame at shoulder height, step through gently until you feel the chest open.",
            ),
            MuscleGroup.BACK to StretchItem(
                "Child's pose with reach",
                MuscleGroup.BACK,
                40,
                "Knees down, hands walking forward, breathe into the ribcage.",
            ),
            MuscleGroup.LATS to StretchItem(
                "Overhead lat stretch",
                MuscleGroup.LATS,
                30,
                "Hands on a bench or bar, hips back, let the armpit lengthen.",
            ),
            MuscleGroup.SHOULDERS to StretchItem(
                "Cross-body shoulder stretch",
                MuscleGroup.SHOULDERS,
                30,
                "Pull the arm across your chest without shrugging the shoulder up.",
            ),
            MuscleGroup.BICEPS to StretchItem(
                "Wall biceps stretch",
                MuscleGroup.BICEPS,
                20,
                "Palm on the wall behind you, rotate the chest away.",
            ),
            MuscleGroup.TRICEPS to StretchItem(
                "Overhead triceps stretch",
                MuscleGroup.TRICEPS,
                30,
                "Elbow to the ceiling, hand down the spine, keep ribs down.",
            ),
            MuscleGroup.QUADS to StretchItem(
                "Standing quad stretch",
                MuscleGroup.QUADS,
                30,
                "Heel to glute, knees together, tuck the pelvis slightly.",
            ),
            MuscleGroup.HAMSTRINGS to StretchItem(
                "Seated hamstring stretch",
                MuscleGroup.HAMSTRINGS,
                40,
                "Hinge from the hips over a straight leg, spine long.",
            ),
            MuscleGroup.GLUTES to StretchItem(
                "Figure-4 glute stretch",
                MuscleGroup.GLUTES,
                40,
                "Ankle over the opposite knee, sit back until the glute releases.",
            ),
            MuscleGroup.CALVES to StretchItem(
                "Wall calf stretch",
                MuscleGroup.CALVES,
                30,
                "Back heel down, knee straight, then repeat with the knee bent.",
            ),
            MuscleGroup.CORE to StretchItem(
                "Cobra stretch",
                MuscleGroup.CORE,
                30,
                "Press the chest up, keep the hips on the floor, breathe.",
            ),
            MuscleGroup.TRAPS to StretchItem(
                "Neck side stretch",
                MuscleGroup.TRAPS,
                20,
                "Ear to shoulder, opposite shoulder relaxed down.",
            ),
        )
    }
}
