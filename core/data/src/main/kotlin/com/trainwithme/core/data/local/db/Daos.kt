package com.trainwithme.core.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}

@Dao
interface MeasurementDao {

    @Query("SELECT * FROM body_measurement ORDER BY recordedAt ASC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM body_measurement ORDER BY recordedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM body_measurement ORDER BY recordedAt DESC LIMIT 1")
    suspend fun latest(): MeasurementEntity?

    @Query("SELECT * FROM body_measurement ORDER BY recordedAt ASC")
    suspend fun getAll(): List<MeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: MeasurementEntity): Long

    @Query("DELETE FROM body_measurement WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM body_measurement")
    suspend fun clear()
}

@Dao
interface PlanDao {

    @Query("SELECT * FROM training_plan WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<PlanEntity?>

    @Query("SELECT * FROM training_plan WHERE isActive = 1 LIMIT 1")
    suspend fun active(): PlanEntity?

    @Query("SELECT * FROM training_plan ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM training_plan ORDER BY createdAt DESC")
    suspend fun getAll(): List<PlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: PlanEntity)

    @Query("UPDATE training_plan SET isActive = 0")
    suspend fun deactivateAll()

    /** Activates a plan atomically: archives the previous one and stores the new snapshot. */
    @Transaction
    suspend fun activate(plan: PlanEntity) {
        deactivateAll()
        insert(plan.copy(isActive = true))
    }

    @Query("DELETE FROM training_plan")
    suspend fun clear()
}

@Dao
interface SessionDao {

    @Query("SELECT * FROM workout_session ORDER BY dateIso DESC, startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM workout_session ORDER BY dateIso ASC, startedAt ASC")
    suspend fun getAll(): List<SessionEntity>

    @Query("SELECT * FROM workout_session WHERE completedAt IS NOT NULL ORDER BY dateIso DESC")
    fun observeCompleted(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM workout_session WHERE dayId = :dayId AND dateIso = :dateIso LIMIT 1")
    suspend fun find(dayId: String, dateIso: String): SessionEntity?

    @Query("SELECT * FROM workout_session WHERE dayId = :dayId AND dateIso = :dateIso LIMIT 1")
    fun observeByDay(dayId: String, dateIso: String): Flow<SessionEntity?>

    @Query("SELECT * FROM workout_session WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    suspend fun findInProgress(): SessionEntity?

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun byId(id: String): SessionEntity?

    @Query("SELECT * FROM workout_session WHERE id = :id")
    fun observeById(id: String): Flow<SessionEntity?>

    @Query("SELECT COUNT(*) FROM workout_session WHERE status = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workout_session WHERE status = 'COMPLETED' AND planId = :planId")
    suspend fun completedCountForPlan(planId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Query("UPDATE workout_session SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun setStatus(id: String, status: String, completedAt: Long?)

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM workout_session")
    suspend fun clear()
}

@Dao
interface SetLogDao {

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId ORDER BY setIndex ASC, id ASC")
    fun observeForSession(sessionId: String): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId ORDER BY setIndex ASC, id ASC")
    suspend fun forSession(sessionId: String): List<SetLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: SetLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<SetLogEntity>)

    @Query("DELETE FROM set_log WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Most recent sets for an exercise before [beforeMillis], newest session first. Feeds the
     * progression engine and the "last time" hint in the player.
     */
    @Query(
        """
        SELECT s.* FROM set_log s
        INNER JOIN workout_session w ON w.id = s.sessionId
        WHERE s.exerciseId = :exerciseId AND s.isWarmup = 0 AND s.completedAt < :beforeMillis
          AND w.status = 'COMPLETED'
        ORDER BY w.dateIso DESC, s.setIndex ASC
        LIMIT 20
        """,
    )
    suspend fun recentForExercise(exerciseId: String, beforeMillis: Long): List<SetLogEntity>

    /** Every working set, joined with its session date — the source for PRs and volume charts. */
    @Query(
        """
        SELECT s.exerciseId AS exerciseId, s.exerciseName AS exerciseName, s.setIndex AS setIndex,
               s.weightKg AS weightKg, s.reps AS reps, s.rpe AS rpe, s.isWarmup AS isWarmup,
               w.dateIso AS dateIso
        FROM set_log s
        INNER JOIN workout_session w ON w.id = s.sessionId
        WHERE w.status = 'COMPLETED'
        ORDER BY w.dateIso ASC
        """,
    )
    fun observeHistory(): Flow<List<HistoryRow>>

    @Query("DELETE FROM set_log")
    suspend fun clear()
}

/** Flat projection used by the progress screen. */
data class HistoryRow(
    val exerciseId: String,
    val exerciseName: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val isWarmup: Boolean,
    val dateIso: String,
)
