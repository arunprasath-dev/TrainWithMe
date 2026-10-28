package com.agon.app.domain.model.profile

import kotlinx.serialization.Serializable

/** Biological sex, used only for BMR/metabolic estimation (Mifflin-St Jeor). */
@Serializable
enum class Sex { MALE, FEMALE }

/** Training experience, drives set counts, exercise difficulty ceiling and progress rate. */
@Serializable
enum class ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;

    val label: String
        get() = when (this) {
            BEGINNER -> "Beginner"
            INTERMEDIATE -> "Intermediate"
            ADVANCED -> "Advanced"
        }

    /** Short helper shown in onboarding. */
    val hint: String
        get() = when (this) {
            BEGINNER -> "New to lifting or returning after 6+ months off"
            INTERMEDIATE -> "Training consistently for 6–24 months"
            ADVANCED -> "2+ years of structured training"
        }
}

/** Self-reported daily activity, multiplies BMR into TDEE. */
@Serializable
enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHTLY_ACTIVE(1.375),
    MODERATELY_ACTIVE(1.55),
    VERY_ACTIVE(1.725),
    EXTREMELY_ACTIVE(1.9);

    val label: String
        get() = when (this) {
            SEDENTARY -> "Sedentary"
            LIGHTLY_ACTIVE -> "Lightly active"
            MODERATELY_ACTIVE -> "Moderately active"
            VERY_ACTIVE -> "Very active"
            EXTREMELY_ACTIVE -> "Extremely active"
        }

    val hint: String
        get() = when (this) {
            SEDENTARY -> "Desk job, under 5,000 steps a day"
            LIGHTLY_ACTIVE -> "On your feet sometimes, 5–8k steps"
            MODERATELY_ACTIVE -> "Active job or 8–12k steps"
            VERY_ACTIVE -> "Physical job or 12k+ steps"
            EXTREMELY_ACTIVE -> "Hard physical labour plus training"
        }
}

/** Primary training goal. Drives split emphasis, prescription and nutrition. */
@Serializable
enum class Goal {
    BUILD_MUSCLE,
    LOSE_FAT,
    RECOMP,
    STRENGTH,
    ENDURANCE;

    val label: String
        get() = when (this) {
            BUILD_MUSCLE -> "Build muscle"
            LOSE_FAT -> "Lose fat"
            RECOMP -> "Recomposition"
            STRENGTH -> "Get stronger"
            ENDURANCE -> "Endurance"
        }

    val hint: String
        get() = when (this) {
            BUILD_MUSCLE -> "Calorie surplus, hypertrophy rep ranges"
            LOSE_FAT -> "Moderate deficit, high protein, short rests"
            RECOMP -> "Maintenance calories, lift heavy, high protein"
            STRENGTH -> "Low reps on main lifts, long rests"
            ENDURANCE -> "High reps, short rests, cardio finishers"
        }

    /** Target body-weight change in kg per week; null when weight change is not the point. */
    val defaultRateKgPerWeek: Double?
        get() = when (this) {
            BUILD_MUSCLE -> 0.25
            LOSE_FAT -> -0.5
            RECOMP, STRENGTH, ENDURANCE -> null
        }
}

/** Equipment the user can actually use. Filters every exercise the engine may pick. */
@Serializable
enum class EquipmentAccess {
    FULL_GYM,
    DUMBBELLS,
    BODYWEIGHT;

    val label: String
        get() = when (this) {
            FULL_GYM -> "Full gym"
            DUMBBELLS -> "Dumbbells at home"
            BODYWEIGHT -> "Bodyweight / bands"
        }

    val hint: String
        get() = when (this) {
            FULL_GYM -> "Barbells, machines, cables, dumbbells"
            DUMBBELLS -> "Dumbbells, a bench and a pull-up bar"
            BODYWEIGHT -> "No weights: bands, floor, pull-up bar"
        }
}

/** Unit system for display only — everything is stored in metric. */
@Serializable
enum class Units { KG, LB }
