package com.trainwithme.feature.progress.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainwithme.core.data.asset.AssetExerciseLibrary
import com.trainwithme.core.data.repository.BodyMeasurement
import com.trainwithme.core.data.repository.MeasurementRepository
import com.trainwithme.core.data.repository.PlanRepository
import com.trainwithme.core.data.repository.ProgressRepository
import com.trainwithme.core.data.repository.SettingsRepository
import com.trainwithme.core.data.repository.WorkoutRepository
import com.trainwithme.core.data.util.TimeProvider
import com.trainwithme.core.model.plan.TrainingPlan
import com.trainwithme.core.model.profile.Circumference
import com.trainwithme.core.model.profile.Units
import com.trainwithme.core.model.progress.PersonalRecord
import com.trainwithme.core.model.progress.ProgressStats
import com.trainwithme.core.model.progress.WeekVolume
import com.trainwithme.core.model.progress.WeightPoint
import com.trainwithme.core.model.training.SessionStatus
import com.trainwithme.core.model.training.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TrendRange(val label: String, val days: Long?) {
    THIRTY("30 days", 30),
    NINETY("90 days", 90),
    ALL("All", null),
}

data class ProgressUiState(
    val isLoading: Boolean = true,
    val units: Units = Units.KG,
    val weightPoints: List<WeightPoint> = emptyList(),
    val bodyFatPoints: List<WeightPoint> = emptyList(),
    val trendKgPerWeek: Double? = null,
    val measurements: List<BodyMeasurement> = emptyList(),
    val personalRecords: List<MeasurementRecord> = emptyList(),
    val weeklyVolume: List<WeekVolume> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val completedSessions: Int = 0,
    val plannedSessions: Int = 0,
    val adherence: Float = 0f,
    val streak: Int = 0,
    val range: TrendRange = TrendRange.NINETY,
) {
    /** Weight points inside the selected range, oldest first. */
    val filteredWeight: List<WeightPoint>
        get() = range.days?.let { days ->
            val cutoff = (weightPoints.maxOfOrNull { it.date } ?: LocalDate.now()).minusDays(days)
            weightPoints.filter { !it.date.isBefore(cutoff) }
        } ?: weightPoints

    val latestWeightKg: Double? get() = weightPoints.lastOrNull()?.weightKg
    val weightChangeKg: Double?
        get() = ProgressStats.deltaKg(filteredWeight.firstOrNull()?.weightKg, filteredWeight.lastOrNull()?.weightKg)

    val waistHistory: List<Pair<LocalDate, Double>>
        get() = measurements.mapNotNull { measurement ->
            measurement.circumference.waistCm?.let { waist ->
                Instant.ofEpochMilli(measurement.recordedAt).atZone(ZoneId.systemDefault()).toLocalDate() to waist
            }
        }
        .sortedBy { it.first }
}

/** A PR with the exercise name resolved for display. */
data class MeasurementRecord(
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val estimatedOneRepMaxKg: Double,
    val achievedOn: LocalDate,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    progressRepository: ProgressRepository,
    private val measurementRepository: MeasurementRepository,
    private val workoutRepository: WorkoutRepository,
    planRepository: PlanRepository,
    private val exerciseLibrary: AssetExerciseLibrary,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val range = MutableStateFlow(TrendRange.NINETY)

    private val training = combine(
        workoutRepository.observeCompletedSessions(),
        planRepository.observeActivePlan(),
    ) { sessions, plan -> sessions to plan }

    val uiState: StateFlow<ProgressUiState> = combine(
        combine(
            progressRepository.observeWeightPoints(),
            progressRepository.observeBodyFatPoints(),
            progressRepository.observePersonalRecords(),
        ) { weights, bodyFat, records -> Triple(weights, bodyFat, records) },
        progressRepository.observeWeeklyVolume(),
        combine(
            measurementRepository.observeAll(),
            training,
        ) { measurements, pair -> measurements to pair },
        settingsRepository.observeSettings(),
        range,
    ) { body, volume, measurementBlock, settings, selectedRange ->
        val (weights, bodyFat, records) = body
        val (measurements, pair) = measurementBlock
        val (sessions, plan) = pair
        buildState(
            weights = weights,
            bodyFat = bodyFat,
            records = records,
            volume = volume,
            measurements = measurements,
            sessions = sessions,
            plan = plan,
            units = settings.units,
            selectedRange = selectedRange,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    private fun buildState(
        weights: List<WeightPoint>,
        bodyFat: List<WeightPoint>,
        records: List<PersonalRecord>,
        volume: List<WeekVolume>,
        measurements: List<BodyMeasurement>,
        sessions: List<WorkoutSession>,
        plan: TrainingPlan?,
        units: Units,
        selectedRange: TrendRange,
    ): ProgressUiState {
        val completed = sessions.filter { it.status == SessionStatus.COMPLETED }
        val planned = plan?.totalPlannedSessions ?: 0
        val streak = plan?.let {
            ProgressStats.currentStreak(
                plannedTrainingDates = it.trainingDays.map { day -> day.date },
                completedDates = completed.map { session -> session.date }.toSet(),
                today = timeProvider.today(),
            )
        } ?: 0
        return ProgressUiState(
            isLoading = false,
            units = units,
            weightPoints = weights,
            bodyFatPoints = bodyFat,
            trendKgPerWeek = ProgressStats.weeklyTrendKgPerWeek(weights),
            measurements = measurements.sortedByDescending { it.recordedAt },
            personalRecords = records.map { record ->
                MeasurementRecord(
                    exerciseName = exerciseLibrary.byId(record.exerciseId)?.name ?: record.exerciseId,
                    weightKg = record.weightKg,
                    reps = record.reps,
                    estimatedOneRepMaxKg = record.estimatedOneRepMaxKg,
                    achievedOn = record.achievedOn,
                )
            },
            weeklyVolume = volume,
            sessions = completed,
            completedSessions = completed.size,
            plannedSessions = planned,
            adherence = ProgressStats.adherence(completed.size, planned),
            streak = streak,
            range = selectedRange,
        )
    }

    fun setRange(newRange: TrendRange) {
        range.value = newRange
    }

    fun addMeasurement(weightKg: Double, bodyFatPercent: Double?, waistCm: Double?) {
        if (weightKg <= 0) return
        viewModelScope.launch {
            measurementRepository.add(
                weightKg = weightKg,
                bodyFatPercent = bodyFatPercent,
                circumference = Circumference(waistCm = waistCm),
                recordedAt = timeProvider.nowMillis(),
                note = null,
            )
        }
    }
}
