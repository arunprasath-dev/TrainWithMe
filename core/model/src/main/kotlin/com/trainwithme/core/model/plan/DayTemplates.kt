package com.trainwithme.core.model.plan

import com.trainwithme.core.model.exercise.MovementPattern
import com.trainwithme.core.model.exercise.MuscleGroup

/**
 * A declarative position in a workout day: which movement pattern belongs there, which muscles it
 * should train, and how important it is when the user's time budget forces truncation.
 *
 * @param priority 0 = never truncated, higher = dropped first.
 * @param alternates patterns that are acceptable substitutes if nothing matches the primary one.
 */
data class SlotSpec(
    val pattern: MovementPattern,
    val primaryMuscles: List<MuscleGroup>,
    val priority: Int,
    val isCompound: Boolean,
    val alternates: List<MovementPattern> = emptyList(),
)

data class DayTemplate(
    val key: String,
    val title: String,
    val focus: List<MuscleGroup>,
    val slots: List<SlotSpec>,
    /** Mobility/activation drills added to the warm-up. */
    val warmupDrills: List<String> = emptyList(),
)

/** A split for one weekly training frequency. */
data class SplitDefinition(
    val name: String,
    val rationale: String,
    val trainingDayIndices: List<Int>,
    val templates: List<DayTemplate>,
) {
    init {
        require(trainingDayIndices.size == templates.size) {
            "Split $name has ${trainingDayIndices.size} training days but ${templates.size} templates"
        }
        require(trainingDayIndices.all { it in 0..6 }) { "Training day indices must be 0..6 (Mon..Sun)" }
    }

    fun templateFor(dayOfWeek: Int): DayTemplate? {
        val index = trainingDayIndices.indexOf(dayOfWeek)
        return if (index >= 0) templates[index] else null
    }
}

/**
 * Split catalogue (docs/PLAN.md §7.3). Frequencies above 3 days rotate an A/B variant so no
 * muscle group is trained with the exact same exercises twice in a week.
 */
object SplitCatalog {

    private val horizontalPush = SlotSpec(
        MovementPattern.HORIZONTAL_PUSH,
        listOf(MuscleGroup.CHEST),
        0,
        true,
    )
    private val verticalPush = SlotSpec(
        MovementPattern.VERTICAL_PUSH,
        listOf(MuscleGroup.SHOULDERS),
        1,
        true,
        alternates = listOf(MovementPattern.HORIZONTAL_PUSH),
    )
    private val horizontalPull = SlotSpec(
        MovementPattern.HORIZONTAL_PULL,
        listOf(MuscleGroup.BACK),
        0,
        true,
    )
    private val verticalPull = SlotSpec(
        MovementPattern.VERTICAL_PULL,
        listOf(MuscleGroup.LATS),
        1,
        true,
        alternates = listOf(MovementPattern.HORIZONTAL_PULL),
    )
    private val squat = SlotSpec(MovementPattern.SQUAT, listOf(MuscleGroup.QUADS), 0, true)
    private val hinge = SlotSpec(
        MovementPattern.HINGE,
        listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
        0,
        true,
        alternates = listOf(MovementPattern.SQUAT),
    )
    private val lunge = SlotSpec(
        MovementPattern.LUNGE,
        listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
        2,
        true,
        alternates = listOf(MovementPattern.SQUAT),
    )
    private val legIsolation = SlotSpec(
        MovementPattern.LEG_ISOLATION,
        listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS),
        3,
        false,
    )
    private val biceps = SlotSpec(MovementPattern.BICEPS_ISOLATION, listOf(MuscleGroup.BICEPS), 3, false)
    private val triceps = SlotSpec(MovementPattern.TRICEPS_ISOLATION, listOf(MuscleGroup.TRICEPS), 3, false)
    private val verticalPushMain = verticalPush.copy(priority = 0)
    private val verticalPullMain = verticalPull.copy(priority = 0)
    private val sideDelts = SlotSpec(
        MovementPattern.SHOULDER_ISOLATION,
        listOf(MuscleGroup.SHOULDERS),
        3,
        false,
    )
    private val calves = SlotSpec(MovementPattern.CALVES, listOf(MuscleGroup.CALVES), 4, false)
    private val core = SlotSpec(MovementPattern.CORE, listOf(MuscleGroup.CORE), 2, false)

    private val fullBodyA = DayTemplate(
        key = "full_body_a",
        title = "Full body A",
        focus = listOf(MuscleGroup.QUADS, MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.CORE),
        slots = listOf(squat, horizontalPush, horizontalPull, hinge, core, sideDelts),
        warmupDrills = listOf("Hip openers", "Deep squat hold", "Band pull-aparts"),
    )
    private val fullBodyB = DayTemplate(
        key = "full_body_b",
        title = "Full body B",
        focus = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.CORE),
        slots = listOf(hinge, verticalPull, verticalPush, lunge, biceps, core),
        warmupDrills = listOf("Glute bridges", "Thoracic rotations", "Scapular pull-downs"),
    )
    private val fullBodyC = DayTemplate(
        key = "full_body_c",
        title = "Full body C",
        focus = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.CHEST, MuscleGroup.BACK),
        slots = listOf(squat, horizontalPush, horizontalPull, legIsolation, triceps, calves),
        warmupDrills = listOf("Walking lunges", "Arm circles", "Cat-cow"),
    )

    private val upperA = DayTemplate(
        key = "upper_a",
        title = "Upper A — push focus",
        focus = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.TRICEPS),
        slots = listOf(horizontalPush, horizontalPull, verticalPush, verticalPull, biceps, triceps),
        warmupDrills = listOf("Band shoulder dislocates", "Scapular push-ups", "Face pulls (light)"),
    )
    private val upperB = DayTemplate(
        key = "upper_b",
        title = "Upper B — pull focus",
        focus = listOf(MuscleGroup.BACK, MuscleGroup.LATS, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS),
        slots = listOf(horizontalPull, verticalPush, verticalPull, horizontalPush, biceps, triceps),
        warmupDrills = listOf("Band pull-aparts", "Dead hangs", "Arm circles"),
    )
    private val lowerA = DayTemplate(
        key = "lower_a",
        title = "Lower A — squat focus",
        focus = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.CORE),
        slots = listOf(squat, hinge, lunge, legIsolation, calves, core),
        warmupDrills = listOf("Hip openers", "Bodyweight squats", "Ankle rocks"),
    )
    private val lowerB = DayTemplate(
        key = "lower_b",
        title = "Lower B — hinge focus",
        focus = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.QUADS, MuscleGroup.CORE),
        slots = listOf(hinge, squat, legIsolation, lunge, calves, core),
        warmupDrills = listOf("Glute bridges", "Hamstring sweeps", "Bodyweight good mornings"),
    )

    private val pushA = DayTemplate(
        key = "push_a",
        title = "Push A",
        focus = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
        slots = listOf(horizontalPush, verticalPush, horizontalPush, sideDelts, triceps, core),
        warmupDrills = listOf("Band shoulder dislocates", "Scapular push-ups"),
    )
    private val pushB = DayTemplate(
        key = "push_b",
        title = "Push B",
        focus = listOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST, MuscleGroup.TRICEPS),
        slots = listOf(verticalPushMain, horizontalPush, sideDelts, horizontalPush, triceps, core),
        warmupDrills = listOf("Arm circles", "Band external rotations"),
    )
    private val pullA = DayTemplate(
        key = "pull_a",
        title = "Pull A",
        focus = listOf(MuscleGroup.LATS, MuscleGroup.BACK, MuscleGroup.BICEPS),
        slots = listOf(verticalPullMain, horizontalPull, horizontalPull, biceps, biceps, core),
        warmupDrills = listOf("Dead hangs", "Band pull-aparts"),
    )
    private val pullB = DayTemplate(
        key = "pull_b",
        title = "Pull B",
        focus = listOf(MuscleGroup.BACK, MuscleGroup.LATS, MuscleGroup.BICEPS),
        slots = listOf(horizontalPull, verticalPull, horizontalPull, biceps, biceps, core),
        warmupDrills = listOf("Scapular pull-downs", "Thoracic rotations"),
    )
    private val legsA = DayTemplate(
        key = "legs_a",
        title = "Legs A",
        focus = listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
        slots = listOf(squat, hinge, lunge, legIsolation, calves, core),
        warmupDrills = listOf("Bodyweight squats", "Hip openers"),
    )
    private val legsB = DayTemplate(
        key = "legs_b",
        title = "Legs B",
        focus = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.QUADS),
        slots = listOf(hinge, lunge, legIsolation, squat, calves, core),
        warmupDrills = listOf("Glute bridges", "Hamstring sweeps"),
    )

    val splits: List<SplitDefinition> = listOf(
        SplitDefinition(
            name = "Full body ×2",
            rationale = "Two full-body sessions hit every muscle group twice a week — the best " +
                "return on two days of training.",
            trainingDayIndices = listOf(0, 3),
            templates = listOf(fullBodyA, fullBodyB),
        ),
        SplitDefinition(
            name = "Full body ×3",
            rationale = "Three full-body sessions keep weekly frequency high while leaving a rest " +
                "day between sessions.",
            trainingDayIndices = listOf(0, 2, 4),
            templates = listOf(fullBodyA, fullBodyB, fullBodyC),
        ),
        SplitDefinition(
            name = "Upper / Lower ×2",
            rationale = "Four days is where upper/lower shines: each muscle group is trained twice " +
                "weekly with enough recovery between sessions.",
            trainingDayIndices = listOf(0, 1, 3, 4),
            templates = listOf(upperA, lowerA, upperB, lowerB),
        ),
        SplitDefinition(
            name = "Push / Pull / Legs + Upper / Lower",
            rationale = "Five days blends a PPL structure with an upper/lower day so no muscle group " +
                "is left under-trained.",
            trainingDayIndices = listOf(0, 1, 2, 4, 5),
            templates = listOf(pushA, pullA, legsA, upperB, lowerB),
        ),
        SplitDefinition(
            name = "Push / Pull / Legs ×2",
            rationale = "Six days allows two rotations of push, pull and legs with a rest day after " +
                "each block.",
            trainingDayIndices = listOf(0, 1, 2, 3, 4, 5),
            templates = listOf(pushA, pullA, legsA, pushB, pullB, legsB),
        ),
    )

    fun forDaysPerWeek(daysPerWeek: Int): SplitDefinition =
        splits.firstOrNull { it.trainingDayIndices.size == daysPerWeek.coerceIn(2, 6) }
            ?: splits.first()
}
