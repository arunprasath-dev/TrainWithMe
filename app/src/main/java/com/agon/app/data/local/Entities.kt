package com.agon.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The single user of the app (id = 1). Kept as one row because v1 is single-profile and every
 * screen reads the same snapshot (docs/PLAN.md §10).
 */
@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String,
    val sex: String,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val bodyFatPercent: Double?,
    val experience: String,
    val activityLevel: String,
    val goal: String,
    val equipmentAccess: String,
    val daysPerWeek: Int,
    val sessionMinutes: Int,
    val goalRateKgPerWeek: Double?,
    val neckCm: Double?,
    val chestCm: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val armCm: Double?,
    val thighCm: Double?,
    val agreedToDisclaimer: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Body-composition time series. Weight/body-fat drive the trend charts and calorie adjustments. */
@Entity(
    tableName = "body_measurement",
    indices = [Index("recordedAt")],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long = 1L,
    val recordedAt: Long,
    val weightKg: Double,
    val bodyFatPercent: Double? = null,
    val neckCm: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null,
    val note: String? = null,
)

/**
 * A generated programme. The week/day/exercise graph is stored as an immutable JSON document in
 * [payloadJson] (header columns exist only for querying); sessions and set logs are normalised
 * because they are written and aggregated per row.
 */
@Entity(
    tableName = "training_plan",
    indices = [Index("isActive"), Index("createdAt")],
)
data class PlanEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val startDateIso: String,
    val splitName: String,
    val weeks: Int,
    val isActive: Boolean,
    val schemaVersion: Int,
    val payloadJson: String,
)

@Entity(
    tableName = "workout_session",
    indices = [
        Index("planId"),
        Index("dayId"),
        Index(value = ["planId", "dayId", "dateIso"], unique = true),
        Index("status"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val dayId: String,
    val weekIndex: Int,
    val dayIndex: Int,
    val dateIso: String,
    val title: String,
    val status: String,
    val startedAt: Long,
    val completedAt: Long?,
)

@Entity(
    tableName = "set_log",
    indices = [Index("sessionId"), Index("exerciseId")],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: String,
    val exerciseId: String,
    val exerciseName: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val isWarmup: Boolean,
    val targetReps: Int?,
    val completedAt: Long,
)
