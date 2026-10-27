package com.trainwithme.feature.plan.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainwithme.core.data.repository.BodyMeasurement
import com.trainwithme.core.data.repository.MeasurementRepository
import com.trainwithme.core.data.repository.PlanRepository
import com.trainwithme.core.data.repository.ProfileRepository
import com.trainwithme.core.data.repository.WorkoutRepository
import com.trainwithme.core.data.util.TimeProvider
import com.trainwithme.core.model.plan.PlanDay
import com.trainwithme.core.model.plan.PlanGenerationResult
import com.trainwithme.core.model.plan.PlanCalendar
import com.trainwithme.core.model.plan.TrainingPlan
import com.trainwithme.core.model.profile.UserProfile
import com.trainwithme.core.model.progress.ProgressStats
import com.trainwithme.core.model.training.SessionStatus
import com.trainwithme.core.model.training.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TodayKind { TRAINING, REST, NO_PLAN }

data class DashboardUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val plan: TrainingPlan? = null,
    val today: PlanDay? = null,
    val todayKind: TodayKind = TodayKind.NO_PLAN,
    val todaySession: WorkoutSession? = null,
    val activeSession: WorkoutSession? = null,
    val weekIndex: Int = 0,
    val weekDays: List<PlanDay> = emptyList(),
    val completedDayIds: Set<String> = emptySet(),
    val completedThisWeek: Int = 0,
    val plannedThisWeek: Int = 0,
    val streak: Int = 0,
    val completedTotal: Int = 0,
    val plannedTotal: Int = 0,
    val latestMeasurement: BodyMeasurement? = null,
    val previousMeasurement: BodyMeasurement? = null,
    val needsPlanRebuild: Boolean = false,
    val isRebuilding: Boolean = false,
    val message: String? = null,
    val todayDate: LocalDate = LocalDate.of(1970, 1, 1),
) {
    val hasPlan: Boolean get() = plan != null
    val adherence: Float
        get() = if (plannedThisWeek == 0) 0f else (completedThisWeek.toFloat() / plannedThisWeek).coerceIn(0f, 1f)
    val weightDeltaKg: Double?
        get() = ProgressStats.deltaKg(previousMeasurement?.weightKg, latestMeasurement?.weightKg)
}

/**
 * Dashboard state is derived, never stored: the day shown, its completion state and the streak all
 * come from the plan + session tables, so nothing can drift out of sync (docs/PLAN.md §6.3 C3).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
    private val measurementRepository: MeasurementRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val transient = MutableStateFlow(TransientState())

    val uiState: StateFlow<DashboardUiState> = combine(
        profileRepository.observeProfile(),
        planRepository.observeActivePlan(),
        workoutRepository.observeSessions(),
        measurementRepository.observeAll(),
        transient,
    ) { profile, plan, sessions, measurements, extra ->
        buildState(profile, plan, sessions, measurements, extra)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    fun refresh() {
        // WorkManager-driven changes (health imports, midnight rollovers) re-read the clock.
        transient.update { it.copy(nowOverride = null) }
    }

    fun dismissMessage() = transient.update { it.copy(message = null) }

    fun logWeight(weightKg: Double) {
        if (weightKg <= 0) return
        viewModelScope.launch {
            measurementRepository.add(
                weightKg = weightKg,
                bodyFatPercent = null,
                recordedAt = timeProvider.nowMillis(),
                note = "Quick weigh-in",
            )
            transient.update { it.copy(message = "Weigh-in saved.") }
        }
    }

    /** Rebuilds the plan after the user changes their body composition or goal. */
    fun rebuildPlan() {
        val profile = uiState.value.profile ?: return
        transient.update { it.copy(isRebuilding = true) }
        viewModelScope.launch {
            val start = PlanCalendar.weekStart(timeProvider.today())
            val result = planRepository.generatePlan(profile.copy(updatedAt = timeProvider.nowMillis()), start)
            transient.update {
                it.copy(
                    isRebuilding = false,
                    message = when (result) {
                        is PlanGenerationResult.Success -> "New plan ready — ${result.plan.splitName}."
                        is PlanGenerationResult.Invalid -> "Your profile needs a fix before rebuilding the plan."
                    },
                )
            }
        }
    }

    private fun buildState(
        profile: UserProfile?,
        plan: TrainingPlan?,
        sessions: List<WorkoutSession>,
        measurements: List<BodyMeasurement>,
        extra: TransientState,
    ): DashboardUiState {
        val today = extra.nowOverride ?: timeProvider.today()
        if (profile == null) {
            return DashboardUiState(isLoading = false, todayKind = TodayKind.NO_PLAN, message = extra.message)
        }
        if (plan == null) {
            return DashboardUiState(
                isLoading = false,
                profile = profile,
                todayKind = TodayKind.NO_PLAN,
                todayDate = today,
                message = extra.message,
            )
        }

        val completed = sessions.filter { it.status == SessionStatus.COMPLETED }
        val todayPlanDay = plan.dayOn(today)
        val todaySession = todayPlanDay?.let { day ->
            sessions.firstOrNull { it.dayId == day.id && it.date == day.date }
        }
        val active = sessions.firstOrNull { it.status == SessionStatus.IN_PROGRESS }

        // If today is a rest day (or falls outside the block) point the user at the next session.
        val upcoming = plan.nextTrainingDayOnOrAfter(today)
        val weekIndex = (todayPlanDay?.weekIndex ?: upcoming?.weekIndex ?: 0).coerceIn(0, plan.weeks.lastIndex)
        val week = plan.weeks[weekIndex]
        val completedDayIds = completed.map { it.dayId }.toSet()
        val weekCompleted = week.days.count { it.id in completedDayIds }
        val plannedThisWeek = week.days.count { !it.isRestDay }

        val sortedMeasurements = measurements.sortedBy { it.recordedAt }
        val latest = sortedMeasurements.lastOrNull()
        val previous = sortedMeasurements.dropLast(1).lastOrNull()

        val streak = ProgressStats.currentStreak(
            plannedTrainingDates = plan.trainingDays.map { it.date },
            completedDates = completed.map { it.date }.toSet(),
            today = today,
        )

        val kind = when {
            todayPlanDay == null -> TodayKind.REST
            todayPlanDay.isRestDay -> TodayKind.REST
            else -> TodayKind.TRAINING
        }

        return DashboardUiState(
            isLoading = false,
            profile = profile,
            plan = plan,
            today = todayPlanDay ?: upcoming,
            todayKind = kind,
            todaySession = todaySession,
            activeSession = active,
            weekIndex = weekIndex,
            weekDays = week.days,
            completedDayIds = completedDayIds,
            completedThisWeek = weekCompleted,
            plannedThisWeek = plannedThisWeek,
            streak = streak,
            completedTotal = completed.size,
            plannedTotal = plan.totalPlannedSessions,
            latestMeasurement = latest,
            previousMeasurement = previous,
            // Profile edited after the plan was generated → offer a rebuild, never do it silently.
            needsPlanRebuild = profile.updatedAt > plan.createdAtMillis,
            isRebuilding = extra.isRebuilding,
            message = extra.message,
            todayDate = today,
        )
    }

    private data class TransientState(
        val message: String? = null,
        val isRebuilding: Boolean = false,
        val nowOverride: LocalDate? = null,
    )
}
