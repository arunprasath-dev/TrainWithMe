package com.agon.app.data.repository

import com.agon.app.domain.model.plan.PlanDay
import com.agon.app.domain.model.plan.PlanGenerationResult
import com.agon.app.domain.model.plan.TrainingPlan
import com.agon.app.domain.model.profile.Circumference
import com.agon.app.domain.model.profile.UserProfile
import com.agon.app.domain.model.progress.PersonalRecord
import com.agon.app.domain.model.progress.WeekVolume
import com.agon.app.domain.model.progress.WeightPoint
import com.agon.app.domain.model.training.SessionSet
import com.agon.app.domain.model.training.WorkoutSession
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import com.agon.app.domain.model.profile.Units

/**
 * Repository contracts consumed by the feature modules. Implementations live in this module and
 * are bound with Hilt, which keeps every ViewModel free of Android/Room types.
 */
interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
    suspend fun clear()
}

interface MeasurementRepository {
    fun observeAll(): Flow<List<BodyMeasurement>>
    fun observeWeightSeries(): Flow<List<WeightPoint>>
    fun observeBodyFatSeries(): Flow<List<WeightPoint>>
    suspend fun latest(): BodyMeasurement?
    suspend fun add(weightKg: Double, bodyFatPercent: Double?, circumference: Circumference = Circumference.EMPTY, note: String? = null, recordedAt: Long)
    suspend fun delete(id: Long)
    suspend fun clear()
}

data class BodyMeasurement(
    val id: Long,
    val recordedAt: Long,
    val weightKg: Double,
    val bodyFatPercent: Double?,
    val circumference: Circumference,
    val note: String?,
)

interface PlanRepository {
    fun observeActivePlan(): Flow<TrainingPlan?>
    suspend fun activePlan(): TrainingPlan?
    /** Generates, persists and activates a plan. Returns a failure result for invalid profiles. */
    suspend fun generatePlan(profile: UserProfile, startDate: LocalDate): PlanGenerationResult
    suspend fun archiveActivePlan()
}

interface WorkoutRepository {
    fun observeSession(sessionId: String): Flow<WorkoutSession?>
    fun observeSessions(): Flow<List<WorkoutSession>>
    fun observeCompletedSessions(): Flow<List<WorkoutSession>>
    fun observeActiveSession(): Flow<WorkoutSession?>
    suspend fun activeSession(): WorkoutSession?
    suspend fun session(sessionId: String): WorkoutSession?
    suspend fun startSession(plan: TrainingPlan, day: PlanDay, startedAt: Long): WorkoutSession
    suspend fun logSet(sessionId: String, set: SessionSet): Long
    suspend fun removeSet(sessionId: String, setRowId: Long)
    suspend fun completeSession(sessionId: String, completedAt: Long)
    suspend fun abandonSession(sessionId: String, endedAt: Long)
    suspend fun previousSets(exerciseId: String, beforeMillis: Long): List<SessionSet>
    /** True when the day already has a completed session (prevents duplicate logging). */
    suspend fun hasCompletedSession(dayId: String, date: LocalDate): Boolean
    suspend fun clear()
}

interface ProgressRepository {
    fun observeWeightPoints(): Flow<List<WeightPoint>>
    fun observeBodyFatPoints(): Flow<List<WeightPoint>>
    fun observePersonalRecords(): Flow<List<PersonalRecord>>
    fun observeWeeklyVolume(): Flow<List<WeekVolume>>
    fun observeHistory(): Flow<List<LoggedHistoryEntry>>
    fun observeCompletedCount(): Flow<Int>
}

data class LoggedHistoryEntry(
    val exerciseId: String,
    val exerciseName: String,
    val date: LocalDate,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val isWarmup: Boolean,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User preferences. Stored in DataStore (not the database) because they are tiny and typed. */
data class UserSettings(
    val units: Units = Units.KG,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val restTimerSoundEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
)

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun current(): UserSettings
    suspend fun setUnits(units: Units)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setReminderTime(hour: Int, minute: Int)
    suspend fun setRestTimerSound(enabled: Boolean)
    suspend fun setKeepScreenOn(enabled: Boolean)
}

/** Single entry point for the destructive "delete everything" action in settings. */
interface DataMaintenanceRepository {
    suspend fun exportJson(): String
    suspend fun deleteAllData()
}
