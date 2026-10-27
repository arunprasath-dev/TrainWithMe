package com.trainwithme.core.model.progress

import com.trainwithme.core.model.training.LoggedSet
import com.trainwithme.core.model.training.ProgressionEngine
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.round

data class WeightPoint(val date: LocalDate, val weightKg: Double)

data class DatedSet(
    val exerciseId: String,
    val date: LocalDate,
    val set: LoggedSet,
)

data class PersonalRecord(
    val exerciseId: String,
    val weightKg: Double,
    val reps: Int,
    val estimatedOneRepMaxKg: Double,
    val achievedOn: LocalDate,
)

data class WeekVolume(val weekStart: LocalDate, val volumeKg: Double, val workingSets: Int)

/**
 * Pure statistics used by the progress screens and the weekly nutrition check.
 * No Android types, no database access — the repository feeds it.
 */
object ProgressStats {

    /** Simple 7-day rolling average, which is the honest way to read a noisy body weight. */
    fun rollingAverage(points: List<WeightPoint>, windowDays: Int = 7): List<WeightPoint> {
        if (points.isEmpty() || windowDays <= 0) return emptyList()
        val sorted = points.sortedBy { it.date }
        return sorted.map { point ->
            val from = point.date.minusDays((windowDays - 1).toLong())
            val window = sorted.filter { !it.date.isBefore(from) && !it.date.isAfter(point.date) }
            WeightPoint(point.date, round1(window.map { it.weightKg }.average()))
        }
    }

    /**
     * Trend in kg/week: mean of the most recent 7 days minus the mean of the 7 days before that,
     * i.e. exactly the number compared against the goal rate in [NutritionCalculator].
     * Returns null until at least three weigh-ins cover both windows.
     */
    fun weeklyTrendKgPerWeek(points: List<WeightPoint>): Double? {
        if (points.isEmpty()) return null
        val sorted = points.sortedBy { it.date }
        val last = sorted.last().date
        val current = sorted.filter { !it.date.isBefore(last.minusDays(6)) }
        val previous = sorted.filter {
            !it.date.isBefore(last.minusDays(13)) && it.date.isBefore(last.minusDays(7))
        }
        if (current.isEmpty() || previous.isEmpty()) return null
        return round2(current.map { it.weightKg }.average() - previous.map { it.weightKg }.average())
    }

    fun adherence(completedSessions: Int, plannedSessions: Int): Double =
        if (plannedSessions <= 0) 0.0 else (completedSessions.toDouble() / plannedSessions).coerceIn(0.0, 1.0)

    /**
     * Consecutive completed sessions walking backwards over planned training days. Today's
     * unfinished session does not break the streak.
     */
    fun currentStreak(
        plannedTrainingDates: List<LocalDate>,
        completedDates: Set<LocalDate>,
        today: LocalDate,
    ): Int {
        val completed = completedDates.toSet()
        val past = plannedTrainingDates
            .filter { !it.isAfter(today) }
            .distinct()
            .sortedDescending()
            .dropWhile { it == today && it !in completed }
        var streak = 0
        for (date in past) {
            if (date in completed) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    fun personalRecords(history: List<DatedSet>): List<PersonalRecord> =
        history.filter { !it.set.isWarmup }
            .groupBy { it.exerciseId }
            .mapNotNull { (exerciseId, sets) ->
                val best = sets.maxByOrNull { ProgressionEngine.epley(it.set.weightKg, it.set.reps) }
                    ?: return@mapNotNull null
                PersonalRecord(
                    exerciseId = exerciseId,
                    weightKg = best.set.weightKg,
                    reps = best.set.reps,
                    estimatedOneRepMaxKg = round1(ProgressionEngine.epley(best.set.weightKg, best.set.reps)),
                    achievedOn = best.date,
                )
            }
            .sortedByDescending { it.estimatedOneRepMaxKg }

    fun weeklyVolume(history: List<DatedSet>): List<WeekVolume> =
        history.filter { !it.set.isWarmup }
            .groupBy { weekStart(it.date) }
            .map { (week, sets) ->
                WeekVolume(
                    weekStart = week,
                    volumeKg = round1(sets.sumOf { it.set.weightKg * it.set.reps }),
                    workingSets = sets.size,
                )
            }
            .sortedBy { it.weekStart }

    /** Compares two body-composition snapshots for the "since you started" card. */
    fun deltaKg(from: Double?, to: Double?): Double? =
        if (from == null || to == null) null else round1(to - from)

    fun isWeightChangeMeaningful(from: Double?, to: Double?): Boolean {
        val delta = deltaKg(from, to) ?: return false
        return abs(delta) >= 0.2
    }

    fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

    private fun round1(v: Double): Double = round(v * 10) / 10
    private fun round2(v: Double): Double = round(v * 100) / 100
}
