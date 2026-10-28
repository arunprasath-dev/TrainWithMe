package com.agon.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.agon.app.TrainWithMeApp

/**
 * Manual ViewModel factory backed by the [TrainWithMeApp] container.
 * Replaces Hilt's @HiltViewModel injection with explicit constructor wiring.
 */
class AppViewModelFactory(
    private val app: TrainWithMeApp,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle: SavedStateHandle = extras.createSavedStateHandle()
        return when {
            modelClass.isAssignableFrom(AppViewModel::class.java) ->
                AppViewModel(app.settingsRepository, app.planRepository, app.reminderScheduler) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(
                    app.profileRepository, app.planRepository, app.workoutRepository,
                    app.measurementRepository, app.settingsRepository, app.timeProvider,
                ) as T
            modelClass.isAssignableFrom(PlanBrowserViewModel::class.java) ->
                PlanBrowserViewModel(app.planRepository, app.workoutRepository) as T
            modelClass.isAssignableFrom(DayDetailViewModel::class.java) ->
                DayDetailViewModel(app.planRepository, app.workoutRepository, savedStateHandle) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    app.settingsRepository, app.planRepository, app.workoutRepository,
                    app.profileRepository, app.dataMaintenanceRepository, app.reminderScheduler,
                ) as T
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
                OnboardingViewModel(app.profileRepository, app.planRepository, app.timeProvider) as T
            modelClass.isAssignableFrom(ProgressViewModel::class.java) ->
                ProgressViewModel(
                    app.progressRepository, app.measurementRepository, app.workoutRepository,
                    app.planRepository, app.exerciseLibrary, app.settingsRepository, app.timeProvider,
                ) as T
            modelClass.isAssignableFrom(WorkoutViewModel::class.java) ->
                WorkoutViewModel(
                    app.planRepository, app.workoutRepository, app.progressRepository,
                    app.settingsRepository, app.exerciseLibrary, app.timeProvider, savedStateHandle,
                ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
        }
    }
}
