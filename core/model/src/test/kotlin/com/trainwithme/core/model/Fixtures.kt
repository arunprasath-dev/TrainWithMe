package com.trainwithme.core.model

import com.trainwithme.core.model.exercise.Equipment
import com.trainwithme.core.model.exercise.Exercise
import com.trainwithme.core.model.exercise.ExerciseLibrary
import com.trainwithme.core.model.exercise.InMemoryExerciseLibrary
import com.trainwithme.core.model.exercise.MovementPattern
import com.trainwithme.core.model.exercise.MuscleGroup
import com.trainwithme.core.model.profile.ActivityLevel
import com.trainwithme.core.model.profile.Circumference
import com.trainwithme.core.model.profile.EquipmentAccess
import com.trainwithme.core.model.profile.ExperienceLevel
import com.trainwithme.core.model.profile.Goal
import com.trainwithme.core.model.profile.Sex
import com.trainwithme.core.model.profile.UserProfile

/**
 * Fast, dependency-free fixtures for engine tests. The *shipped* catalogue lives in
 * `core/data/src/main/assets/exercises.json` and is validated by tests in `:core:data`.
 */
internal object Fixtures {

    fun exercise(
        id: String,
        pattern: MovementPattern,
        primary: List<MuscleGroup>,
        equipment: Set<Equipment>,
        difficulty: Int = 2,
        compound: Boolean = true,
        secondary: List<MuscleGroup> = emptyList(),
    ): Exercise = Exercise(
        id = id,
        name = id.replace('-', ' '),
        pattern = pattern,
        primaryMuscles = primary,
        secondaryMuscles = secondary,
        equipment = equipment,
        difficulty = difficulty,
        isCompound = compound,
        cues = listOf("Brace.", "Move with control."),
    )

    val allExercises: List<Exercise> = listOf(
        // Horizontal push
        exercise("bench-press", MovementPattern.HORIZONTAL_PUSH, listOf(MuscleGroup.CHEST), setOf(Equipment.BARBELL, Equipment.BENCH), 3),
        exercise("db-press", MovementPattern.HORIZONTAL_PUSH, listOf(MuscleGroup.CHEST), setOf(Equipment.DUMBBELL, Equipment.BENCH), 2),
        exercise("push-up", MovementPattern.HORIZONTAL_PUSH, listOf(MuscleGroup.CHEST), setOf(Equipment.BODYWEIGHT), 1),
        exercise("cable-fly", MovementPattern.HORIZONTAL_PUSH, listOf(MuscleGroup.CHEST), setOf(Equipment.CABLE), 2, compound = false),
        // Vertical push
        exercise("ohp", MovementPattern.VERTICAL_PUSH, listOf(MuscleGroup.SHOULDERS), setOf(Equipment.BARBELL), 3),
        exercise("db-shoulder-press", MovementPattern.VERTICAL_PUSH, listOf(MuscleGroup.SHOULDERS), setOf(Equipment.DUMBBELL), 2),
        exercise("pike-push-up", MovementPattern.VERTICAL_PUSH, listOf(MuscleGroup.SHOULDERS), setOf(Equipment.BODYWEIGHT), 3),
        // Horizontal pull
        exercise("barbell-row", MovementPattern.HORIZONTAL_PULL, listOf(MuscleGroup.BACK), setOf(Equipment.BARBELL), 3),
        exercise("db-row", MovementPattern.HORIZONTAL_PULL, listOf(MuscleGroup.BACK), setOf(Equipment.DUMBBELL), 2),
        exercise("inverted-row", MovementPattern.HORIZONTAL_PULL, listOf(MuscleGroup.BACK), setOf(Equipment.PULLUP_BAR), 2),
        // Vertical pull
        exercise("lat-pulldown", MovementPattern.VERTICAL_PULL, listOf(MuscleGroup.LATS), setOf(Equipment.CABLE), 1),
        exercise("pull-up", MovementPattern.VERTICAL_PULL, listOf(MuscleGroup.LATS), setOf(Equipment.PULLUP_BAR), 4),
        exercise("band-pulldown", MovementPattern.VERTICAL_PULL, listOf(MuscleGroup.LATS), setOf(Equipment.BAND), 1),
        // Squat
        exercise("back-squat", MovementPattern.SQUAT, listOf(MuscleGroup.QUADS), setOf(Equipment.BARBELL), 4),
        exercise("goblet-squat", MovementPattern.SQUAT, listOf(MuscleGroup.QUADS), setOf(Equipment.DUMBBELL), 2),
        exercise("bodyweight-squat", MovementPattern.SQUAT, listOf(MuscleGroup.QUADS), setOf(Equipment.BODYWEIGHT), 1),
        // Hinge
        exercise("deadlift", MovementPattern.HINGE, listOf(MuscleGroup.HAMSTRINGS), setOf(Equipment.BARBELL), 5),
        exercise("db-rdl", MovementPattern.HINGE, listOf(MuscleGroup.HAMSTRINGS), setOf(Equipment.DUMBBELL), 2),
        exercise("glute-bridge", MovementPattern.HINGE, listOf(MuscleGroup.GLUTES), setOf(Equipment.BODYWEIGHT), 1),
        // Lunge
        exercise("walking-lunge", MovementPattern.LUNGE, listOf(MuscleGroup.QUADS), setOf(Equipment.DUMBBELL), 2),
        exercise("reverse-lunge", MovementPattern.LUNGE, listOf(MuscleGroup.QUADS), setOf(Equipment.BODYWEIGHT), 1),
        // Leg isolation
        exercise("leg-curl", MovementPattern.LEG_ISOLATION, listOf(MuscleGroup.HAMSTRINGS), setOf(Equipment.MACHINE), 1, compound = false),
        exercise("band-hamstring-curl", MovementPattern.LEG_ISOLATION, listOf(MuscleGroup.HAMSTRINGS), setOf(Equipment.BAND), 2, compound = false),
        // Biceps
        exercise("db-curl", MovementPattern.BICEPS_ISOLATION, listOf(MuscleGroup.BICEPS), setOf(Equipment.DUMBBELL), 1, compound = false),
        exercise("band-curl", MovementPattern.BICEPS_ISOLATION, listOf(MuscleGroup.BICEPS), setOf(Equipment.BAND), 1, compound = false),
        // Triceps
        exercise("cable-pushdown", MovementPattern.TRICEPS_ISOLATION, listOf(MuscleGroup.TRICEPS), setOf(Equipment.CABLE), 1, compound = false),
        exercise("band-pushdown", MovementPattern.TRICEPS_ISOLATION, listOf(MuscleGroup.TRICEPS), setOf(Equipment.BAND), 1, compound = false),
        // Shoulders
        exercise("lateral-raise", MovementPattern.SHOULDER_ISOLATION, listOf(MuscleGroup.SHOULDERS), setOf(Equipment.DUMBBELL), 1, compound = false),
        exercise("band-lateral-raise", MovementPattern.SHOULDER_ISOLATION, listOf(MuscleGroup.SHOULDERS), setOf(Equipment.BAND), 1, compound = false),
        // Calves
        exercise("calf-raise", MovementPattern.CALVES, listOf(MuscleGroup.CALVES), setOf(Equipment.MACHINE), 1, compound = false),
        exercise("bodyweight-calf-raise", MovementPattern.CALVES, listOf(MuscleGroup.CALVES), setOf(Equipment.BODYWEIGHT), 1, compound = false),
        // Core
        exercise("plank", MovementPattern.CORE, listOf(MuscleGroup.CORE), setOf(Equipment.BODYWEIGHT), 1, compound = false),
        exercise("cable-crunch", MovementPattern.CORE, listOf(MuscleGroup.CORE), setOf(Equipment.CABLE), 2, compound = false),
    )

    val library: ExerciseLibrary = InMemoryExerciseLibrary(allExercises)

    fun profile(
        goal: Goal = Goal.BUILD_MUSCLE,
        experience: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
        equipment: EquipmentAccess = EquipmentAccess.FULL_GYM,
        days: Int = 4,
        minutes: Int = 60,
        weightKg: Double = 78.0,
        heightCm: Double = 178.0,
        age: Int = 28,
        sex: Sex = Sex.MALE,
        bodyFat: Double? = 24.0,
        rate: Double? = null,
        circumferences: Circumference = Circumference.EMPTY,
    ): UserProfile = UserProfile(
        name = "Test Athlete",
        sex = sex,
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        bodyFatPercent = bodyFat,
        experience = experience,
        activityLevel = ActivityLevel.MODERATELY_ACTIVE,
        goal = goal,
        equipmentAccess = equipment,
        daysPerWeek = days,
        sessionMinutes = minutes,
        goalRateKgPerWeek = rate,
        circumferences = circumferences,
        agreedToDisclaimer = true,
    )
}
