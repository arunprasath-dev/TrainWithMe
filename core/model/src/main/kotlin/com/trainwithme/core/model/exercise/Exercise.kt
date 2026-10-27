package com.trainwithme.core.model.exercise

import com.trainwithme.core.model.profile.EquipmentAccess

import kotlinx.serialization.Serializable

/** Muscle groups tracked for volume targeting and UI grouping. */
@Serializable
enum class MuscleGroup {
    CHEST,
    BACK,
    LATS,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    CORE,
    TRAPS,
    FOREARMS;

    val label: String
        get() = when (this) {
            CHEST -> "Chest"
            BACK -> "Back"
            LATS -> "Lats"
            SHOULDERS -> "Shoulders"
            BICEPS -> "Biceps"
            TRICEPS -> "Triceps"
            QUADS -> "Quads"
            HAMSTRINGS -> "Hamstrings"
            GLUTES -> "Glutes"
            CALVES -> "Calves"
            CORE -> "Core"
            TRAPS -> "Traps"
            FOREARMS -> "Forearms"
        }

    /** Groups that are trained together for the weekly volume audit. */
    val volumeFamily: VolumeFamily
        get() = when (this) {
            CHEST -> VolumeFamily.CHEST
            BACK, LATS -> VolumeFamily.BACK
            SHOULDERS -> VolumeFamily.SHOULDERS
            BICEPS -> VolumeFamily.BICEPS
            TRICEPS -> VolumeFamily.TRICEPS
            QUADS -> VolumeFamily.QUADS
            HAMSTRINGS -> VolumeFamily.HAMSTRINGS
            GLUTES -> VolumeFamily.GLUTES
            CALVES -> VolumeFamily.CALVES
            CORE -> VolumeFamily.CORE
            TRAPS -> VolumeFamily.BACK
            // Forearm work is arm work: it counts towards the biceps budget instead of needing a
            // weekly target of its own (the validator's families are the training split's groups).
            FOREARMS -> VolumeFamily.BICEPS
        }
}

/** Coarse muscle buckets used by the weekly volume validator (§B5 of the plan). */
@Serializable
enum class VolumeFamily { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, QUADS, HAMSTRINGS, GLUTES, CALVES, CORE }

/** Movement patterns. Every slot in a day template names one of these. */
@Serializable
enum class MovementPattern {
    HORIZONTAL_PUSH,
    VERTICAL_PUSH,
    HORIZONTAL_PULL,
    VERTICAL_PULL,
    SQUAT,
    HINGE,
    LUNGE,
    LEG_ISOLATION,
    BICEPS_ISOLATION,
    TRICEPS_ISOLATION,
    SHOULDER_ISOLATION,
    CALVES,
    CORE,
    CONDITIONING;

    val label: String
        get() = when (this) {
            HORIZONTAL_PUSH -> "Horizontal push"
            VERTICAL_PUSH -> "Vertical push"
            HORIZONTAL_PULL -> "Horizontal pull"
            VERTICAL_PULL -> "Vertical pull"
            SQUAT -> "Squat"
            HINGE -> "Hinge"
            LUNGE -> "Lunge"
            LEG_ISOLATION -> "Leg isolation"
            BICEPS_ISOLATION -> "Biceps"
            TRICEPS_ISOLATION -> "Triceps"
            SHOULDER_ISOLATION -> "Shoulder isolation"
            CALVES -> "Calves"
            CORE -> "Core"
            CONDITIONING -> "Conditioning"
        }

    /** Asset name of the demo clip for this pattern (see docs/CONTENT.md). */
    val clipId: String
        get() = name.lowercase()
}

/** Concrete implements required to perform an exercise. */
@Serializable
enum class Equipment {
    BARBELL,
    DUMBBELL,
    MACHINE,
    CABLE,
    SMITH_MACHINE,
    BENCH,
    PULLUP_BAR,
    KETTLEBELL,
    BAND,
    BODYWEIGHT;

    val label: String
        get() = when (this) {
            BARBELL -> "Barbell"
            DUMBBELL -> "Dumbbells"
            MACHINE -> "Machine"
            CABLE -> "Cable"
            SMITH_MACHINE -> "Smith machine"
            BENCH -> "Bench"
            PULLUP_BAR -> "Pull-up bar"
            KETTLEBELL -> "Kettlebell"
            BAND -> "Band"
            BODYWEIGHT -> "Bodyweight"
        }
}

/** Which equipment sets each access tier unlocks. */
val EquipmentAccess.allowedEquipment: Set<Equipment>
    get() = when (this) {
        EquipmentAccess.FULL_GYM -> setOf(
            Equipment.BARBELL,
            Equipment.DUMBBELL,
            Equipment.MACHINE,
            Equipment.CABLE,
            Equipment.SMITH_MACHINE,
            Equipment.BENCH,
            Equipment.PULLUP_BAR,
            Equipment.KETTLEBELL,
            Equipment.BAND,
            Equipment.BODYWEIGHT,
        )
        EquipmentAccess.DUMBBELLS -> setOf(
            Equipment.DUMBBELL,
            Equipment.BENCH,
            Equipment.PULLUP_BAR,
            Equipment.BAND,
            Equipment.BODYWEIGHT,
        )
        EquipmentAccess.BODYWEIGHT -> setOf(
            Equipment.PULLUP_BAR,
            Equipment.BAND,
            Equipment.BODYWEIGHT,
        )
    }

/**
 * One movement in the catalogue.
 *
 * @param videoAsset file name (without folder) of the demo clip; when absent the player falls
 *   back to the clip for [pattern].
 * @param cues step-by-step technique instructions, one action per step.
 * @param mistakes the errors coaches correct most often.
 */
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val pattern: MovementPattern,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: Set<Equipment>,
    /** 1 = easiest to learn, 5 = advanced technical demand. */
    val difficulty: Int,
    val isCompound: Boolean,
    val tempo: String = "2-0-1",
    val defaultRestSeconds: Int = 90,
    val setup: String = "",
    val cues: List<String>,
    val mistakes: List<String> = emptyList(),
    val breathing: String = "Exhale through the hardest part of the rep.",
    val safetyNotes: String? = null,
    val videoAsset: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Exercise id must not be blank" }
        require(difficulty in 1..5) { "Difficulty must be in 1..5 (was $difficulty for $id)" }
        require(cues.isNotEmpty()) { "Exercise $id needs at least one technique cue" }
        require(primaryMuscles.isNotEmpty()) { "Exercise $id needs a primary muscle" }
    }

    /** Stable id of the demo clip to play in the player. */
    val videoClipId: String
        get() = videoAsset?.removeSuffix(".mp4") ?: pattern.clipId

    fun isPerformableWith(access: EquipmentAccess): Boolean =
        access.allowedEquipment.containsAll(equipment)
}

/** A read-only catalogue. Implementations must be deterministic in [all]. */
interface ExerciseLibrary {
    fun all(): List<Exercise>
    fun byId(id: String): Exercise?
    fun byPattern(pattern: MovementPattern): List<Exercise> = all().filter { it.pattern == pattern }
}

/** In-memory library, used by tests and as a fallback if content parsing fails. */
class InMemoryExerciseLibrary(exercises: List<Exercise>) : ExerciseLibrary {
    private val ordered: List<Exercise> = exercises.sortedBy { it.id }
    private val index: Map<String, Exercise> = ordered.associateBy { it.id }

    init {
        require(index.size == ordered.size) { "Duplicate exercise ids in library" }
    }

    override fun all(): List<Exercise> = ordered
    override fun byId(id: String): Exercise? = index[id]
}
