package com.agon.app.domain.model.training

import java.time.LocalDate

enum class SessionStatus { IN_PROGRESS, COMPLETED, ABANDONED }

/** One set the user logged inside a session. [id] is 0 until it is persisted. */
data class SessionSet(
    val id: Long = 0L,
    val exerciseId: String,
    val exerciseName: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
    val targetReps: Int? = null,
    val completedAt: Long = 0L,
) {
    fun toLoggedSet(): LoggedSet = LoggedSet(setIndex, weightKg, reps, rpe, isWarmup)

    val volumeKg: Double get() = weightKg * reps
}

/** A training session, either in progress or finished. */
data class WorkoutSession(
    val id: String,
    val planId: String,
    val dayId: String,
    val weekIndex: Int,
    val dayIndex: Int,
    val date: LocalDate,
    val title: String,
    val status: SessionStatus,
    val startedAt: Long,
    val completedAt: Long? = null,
    val sets: List<SessionSet> = emptyList(),
) {
    val workingSets: List<SessionSet> get() = sets.filter { !it.isWarmup }

    val volumeKg: Double get() = workingSets.sumOf { it.volumeKg }

    val durationMinutes: Int get() = (((completedAt ?: zeroIfUnknown()) - startedAt).coerceAtLeast(0) / 60_000).toInt()

    fun setsFor(exerciseId: String): List<SessionSet> =
        sets.filter { it.exerciseId == exerciseId }.sortedBy { it.setIndex }

    fun isCompleted(): Boolean = status == SessionStatus.COMPLETED

    private fun zeroIfUnknown(): Long = startedAt
}

/** Aggregated numbers shown on the session summary screen. */
data class SessionSummary(
    val sessionId: String,
    val title: String,
    val date: LocalDate,
    val durationMinutes: Int,
    val workingSets: Int,
    val totalReps: Int,
    val volumeKg: Double,
    val personalRecords: List<PersonalRecordDelta>,
    val plannedSets: Int,
)

/** A PR hit during a finished session, used to celebrate specific lifts. */
data class PersonalRecordDelta(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val estimatedOneRepMaxKg: Double,
    val previousBestKg: Double?,
)
