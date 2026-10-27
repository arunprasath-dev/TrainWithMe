package com.trainwithme.core.data.di

import android.content.Context
import com.trainwithme.core.data.local.db.MeasurementDao
import com.trainwithme.core.data.local.db.PlanDao
import com.trainwithme.core.data.local.db.ProfileDao
import com.trainwithme.core.data.local.db.SessionDao
import com.trainwithme.core.data.local.db.SetLogDao
import com.trainwithme.core.data.local.db.TrainWithMeDatabase
import com.trainwithme.core.data.reminder.DefaultReminderScheduler
import com.trainwithme.core.data.reminder.ReminderScheduler
import com.trainwithme.core.data.repository.DataMaintenanceRepository
import com.trainwithme.core.data.repository.DefaultDataMaintenanceRepository
import com.trainwithme.core.data.repository.DefaultMeasurementRepository
import com.trainwithme.core.data.repository.DefaultPlanRepository
import com.trainwithme.core.data.repository.DefaultProfileRepository
import com.trainwithme.core.data.repository.DefaultProgressRepository
import com.trainwithme.core.data.repository.DefaultSettingsRepository
import com.trainwithme.core.data.repository.DefaultWorkoutRepository
import com.trainwithme.core.data.repository.MeasurementRepository
import com.trainwithme.core.data.repository.PlanRepository
import com.trainwithme.core.data.repository.ProfileRepository
import com.trainwithme.core.data.repository.ProgressRepository
import com.trainwithme.core.data.repository.SettingsRepository
import com.trainwithme.core.data.repository.WorkoutRepository
import com.trainwithme.core.data.util.SystemTimeProvider
import com.trainwithme.core.data.util.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Dispatcher qualifiers so every unit test can swap in a TestDispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrainWithMeDatabase =
        TrainWithMeDatabase.build(context)

    @Provides
    fun provideProfileDao(database: TrainWithMeDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideMeasurementDao(database: TrainWithMeDatabase): MeasurementDao = database.measurementDao()

    @Provides
    fun providePlanDao(database: TrainWithMeDatabase): PlanDao = database.planDao()

    @Provides
    fun provideSessionDao(database: TrainWithMeDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideSetLogDao(database: TrainWithMeDatabase): SetLogDao = database.setLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun profileRepository(impl: DefaultProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun measurementRepository(impl: DefaultMeasurementRepository): MeasurementRepository

    @Binds
    @Singleton
    abstract fun planRepository(impl: DefaultPlanRepository): PlanRepository

    @Binds
    @Singleton
    abstract fun workoutRepository(impl: DefaultWorkoutRepository): WorkoutRepository

    @Binds
    @Singleton
    abstract fun progressRepository(impl: DefaultProgressRepository): ProgressRepository

    @Binds
    @Singleton
    abstract fun settingsRepository(impl: DefaultSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun dataMaintenanceRepository(impl: DefaultDataMaintenanceRepository): DataMaintenanceRepository

    @Binds
    @Singleton
    abstract fun timeProvider(impl: SystemTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun reminderScheduler(impl: DefaultReminderScheduler): ReminderScheduler
}
