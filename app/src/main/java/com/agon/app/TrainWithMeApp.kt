package com.agon.app

import android.app.Application
import com.agon.app.data.asset.AssetExerciseLibrary
import com.agon.app.data.local.TrainWithMeDatabase
import com.agon.app.data.reminder.DefaultReminderScheduler
import com.agon.app.data.reminder.ReminderScheduler
import com.agon.app.data.repository.DataMaintenanceRepository
import com.agon.app.data.repository.DefaultDataMaintenanceRepository
import com.agon.app.data.repository.DefaultMeasurementRepository
import com.agon.app.data.repository.DefaultPlanRepository
import com.agon.app.data.repository.DefaultProfileRepository
import com.agon.app.data.repository.DefaultProgressRepository
import com.agon.app.data.repository.DefaultSettingsRepository
import com.agon.app.data.repository.DefaultWorkoutRepository
import com.agon.app.data.repository.MeasurementRepository
import com.agon.app.data.repository.PlanRepository
import com.agon.app.data.repository.ProfileRepository
import com.agon.app.data.repository.ProgressRepository
import com.agon.app.data.repository.SettingsRepository
import com.agon.app.data.repository.WorkoutRepository
import com.agon.app.data.util.SystemTimeProvider
import com.agon.app.data.util.TimeProvider
import kotlinx.coroutines.Dispatchers

/**
 * Manual dependency container (service locator). The original multi-module app used Hilt; this
 * single-module rebuild wires the exact same repository graph by hand so ViewModels and Workers
 * share one instance of every dependency.
 */
class TrainWithMeApp : Application() {

    val timeProvider: TimeProvider by lazy { SystemTimeProvider() }

    val database: TrainWithMeDatabase by lazy { TrainWithMeDatabase.build(this) }

    val exerciseLibrary: AssetExerciseLibrary by lazy { AssetExerciseLibrary(this) }

    val profileRepository: ProfileRepository by lazy {
        DefaultProfileRepository(database.profileDao(), timeProvider)
    }

    val measurementRepository: MeasurementRepository by lazy {
        DefaultMeasurementRepository(database.measurementDao(), timeProvider)
    }

    val planRepository: PlanRepository by lazy {
        DefaultPlanRepository(database.planDao(), exerciseLibrary, timeProvider, Dispatchers.IO)
    }

    val workoutRepository: WorkoutRepository by lazy {
        DefaultWorkoutRepository(database.sessionDao(), database.setLogDao(), timeProvider, Dispatchers.IO)
    }

    val progressRepository: ProgressRepository by lazy {
        DefaultProgressRepository(database.setLogDao(), database.measurementDao(), database.sessionDao(), Dispatchers.IO)
    }

    val settingsRepository: SettingsRepository by lazy { DefaultSettingsRepository(this) }

    val dataMaintenanceRepository: DataMaintenanceRepository by lazy {
        DefaultDataMaintenanceRepository(
            database.profileDao(),
            database.measurementDao(),
            database.planDao(),
            database.sessionDao(),
            database.setLogDao(),
            timeProvider,
            Dispatchers.IO,
        )
    }

    val reminderScheduler: ReminderScheduler by lazy { DefaultReminderScheduler(this) }
}

/** Convenience accessor from any Context (Activities, Workers, ViewModel factories). */
fun android.content.Context.app(): TrainWithMeApp = applicationContext as TrainWithMeApp
