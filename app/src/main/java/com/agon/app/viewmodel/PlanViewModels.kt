package com.agon.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.data.reminder.ReminderScheduler
import com.agon.app.data.repository.DataMaintenanceRepository
import com.agon.app.data.repository.PlanRepository
import com.agon.app.data.repository.ProfileRepository
import com.agon.app.data.repository.SettingsRepository
import com.agon.app.data.repository.ThemeMode
import com.agon.app.data.repository.UserSettings
import com.agon.app.data.repository.WorkoutRepository
import com.agon.app.domain.model.plan.PlanDay
import com.agon.app.domain.model.plan.PlanWeek
import com.agon.app.domain.model.plan.TrainingPlan
import com.agon.app.domain.model.profile.Units
import com.agon.app.domain.model.training.SessionStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Plan browser
// ---------------------------------------------------------------------------------------------

data class PlanBrowserUiState(
    val isLoading: Boolean = true,
    val plan: TrainingPlan? = null,
    val selectedWeekIndex: Int = 0,
    val completedDayIds: Set<String> = emptySet(),
    val week: PlanWeek? = null,
    val isDeload: Boolean = false,
) {
    val weekTabs: List<Int> get() = plan?.weeks?.indices?.toList().orEmpty()
}


class PlanBrowserViewModel (
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val selectedWeek = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<PlanBrowserUiState> = combine(
        planRepository.observeActivePlan(),
        workoutRepository.observeCompletedSessions(),
        selectedWeek,
    ) { plan, sessions, selected ->
        if (plan == null) {
            PlanBrowserUiState(isLoading = false)
        } else {
            val weekIndex = (selected ?: currentWeekIndex(plan)).coerceIn(0, plan.weeks.lastIndex)
            PlanBrowserUiState(
                isLoading = false,
                plan = plan,
                selectedWeekIndex = weekIndex,
                completedDayIds = sessions.filter { it.status == SessionStatus.COMPLETED }.map { it.dayId }.toSet(),
                week = plan.weeks[weekIndex],
                isDeload = plan.weeks[weekIndex].isDeload,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanBrowserUiState())

    fun selectWeek(index: Int) {
        selectedWeek.value = index
    }

    /** The week containing today, or the last week once the block is over. */
    private fun currentWeekIndex(plan: TrainingPlan): Int {
        val today = LocalDate.now()
        val index = plan.weeks.indexOfFirst { week -> week.days.any { it.date == today } }
        if (index >= 0) return index
        val upcoming = plan.trainingDays.firstOrNull { it.date.isAfter(today) }
        return when {
            upcoming != null -> upcoming.weekIndex
            else -> plan.weeks.lastIndex
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Day detail
// ---------------------------------------------------------------------------------------------

data class DayDetailUiState(
    val isLoading: Boolean = true,
    val plan: TrainingPlan? = null,
    val day: PlanDay? = null,
    val weekLabel: String = "",
    val isCompleted: Boolean = false,
    val isToday: Boolean = false,
) {
    val title: String get() = day?.title ?: "Day"
}


class DayDetailViewModel (
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val dayId: String = savedStateHandle.get<String>("dayId").orEmpty()

    val uiState: StateFlow<DayDetailUiState> = combine(
        planRepository.observeActivePlan(),
        workoutRepository.observeCompletedSessions(),
    ) { plan, sessions ->
        val day = plan?.dayById(dayId)
        DayDetailUiState(
            isLoading = false,
            plan = plan,
            day = day,
            weekLabel = plan?.weekOf(dayId)?.let { "Week ${it.index + 1} · ${it.label}" }.orEmpty(),
            isCompleted = sessions.any { it.dayId == dayId && it.status == SessionStatus.COMPLETED },
            isToday = day?.date == LocalDate.now(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayDetailUiState())
}

// ---------------------------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------------------------

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val profileName: String? = null,
    val planSummary: String? = null,
    val completedSessions: Int = 0,
    val exporting: Boolean = false,
    val exportPayload: String? = null,
    val confirmingDelete: Boolean = false,
    val message: String? = null,
)


class SettingsViewModel (
    private val settingsRepository: SettingsRepository,
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
    private val dataMaintenanceRepository: DataMaintenanceRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeSettings(),
        profileRepository.observeProfile(),
        planRepository.observeActivePlan(),
        workoutRepository.observeCompletedSessions(),
        transient,
    ) { settings, profile, plan, sessions, extra ->
        SettingsUiState(
            settings = settings,
            profileName = profile?.name,
            planSummary = plan?.let { "${it.splitName} · started ${it.startDateIso}" },
            completedSessions = sessions.size,
            exporting = extra.exporting,
            exportPayload = extra.exportPayload,
            confirmingDelete = extra.confirmingDelete,
            message = extra.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setUnits(units: Units) = launch { settingsRepository.setUnits(units) }

    fun setTheme(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }

    fun setRemindersEnabled(enabled: Boolean) = launch {
        settingsRepository.setRemindersEnabled(enabled)
        val current = settingsRepository.current()
        reminderScheduler.reschedule(enabled, current.reminderHour, current.reminderMinute)
    }

    fun setReminderTime(hour: Int, minute: Int) = launch {
        settingsRepository.setReminderTime(hour, minute)
        val current = settingsRepository.current()
        reminderScheduler.reschedule(current.remindersEnabled, hour, minute)
    }

    fun setRestSound(enabled: Boolean) = launch { settingsRepository.setRestTimerSound(enabled) }

    fun setKeepScreenOn(enabled: Boolean) = launch { settingsRepository.setKeepScreenOn(enabled) }

    fun requestDelete(confirming: Boolean) = transient.update { it.copy(confirmingDelete = confirming) }

    fun exportData(onReady: (String) -> Unit) {
        transient.update { it.copy(exporting = true) }
        viewModelScope.launch {
            val payload = runCatching { dataMaintenanceRepository.exportJson() }.getOrNull()
            transient.update { it.copy(exporting = false, exportPayload = payload) }
            if (payload != null) onReady(payload) else transient.update { it.copy(message = "Export failed.") }
        }
    }

    fun deleteEverything(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { dataMaintenanceRepository.deleteAllData() }
                .onSuccess {
                    reminderScheduler.cancel()
                    onDeleted()
                }
                .onFailure { transient.update { it.copy(message = "Could not delete all data.") } }
        }
    }

    fun dismissMessage() = transient.update { it.copy(message = null) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }

    private data class Transient(
        val exporting: Boolean = false,
        val exportPayload: String? = null,
        val confirmingDelete: Boolean = false,
        val message: String? = null,
    )
}
