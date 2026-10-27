package com.trainwithme.core.data.repository

import com.trainwithme.core.data.di.IoDispatcher
import com.trainwithme.core.data.local.db.SessionDao
import com.trainwithme.core.data.local.db.SessionEntity
import com.trainwithme.core.data.local.db.SetLogDao
import com.trainwithme.core.data.util.TimeProvider
import com.trainwithme.core.model.plan.PlanDay
import com.trainwithme.core.model.plan.TrainingPlan
import com.trainwithme.core.model.training.SessionSet
import com.trainwithme.core.model.training.SessionStatus
import com.trainwithme.core.model.training.WorkoutSession
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class DefaultWorkoutRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val setLogDao: SetLogDao,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WorkoutRepository {

    override fun observeSession(sessionId: String): Flow<WorkoutSession?> =
        combine(
            sessionDao.observeById(sessionId),
            setLogDao.observeForSession(sessionId),
        ) { session, sets -> session?.let { Mappers.toSession(it, sets) } }

    // Summary-level streams intentionally carry no set rows: lists never render per-set detail,
    // and loading every set for every session would make the history screen O(all sets).
    override fun observeSessions(): Flow<List<WorkoutSession>> =
        sessionDao.observeAll().map { sessions -> sessions.map { Mappers.toSession(it, emptyList()) } }

    override fun observeCompletedSessions(): Flow<List<WorkoutSession>> =
        sessionDao.observeCompleted().map { sessions -> sessions.map { Mappers.toSession(it, emptyList()) } }

    override fun observeActiveSession(): Flow<WorkoutSession?> = sessionDao.observeAll().map { sessions ->
        sessions.firstOrNull { it.status == SessionStatus.IN_PROGRESS.name }
            ?.let { Mappers.toSession(it, emptyList()) }
    }


    override suspend fun activeSession(): WorkoutSession? = withContext(ioDispatcher) {
        sessionDao.findInProgress()?.let { session ->
            Mappers.toSession(session, setLogDao.forSession(session.id))
        }
    }

    override suspend fun session(sessionId: String): WorkoutSession? = withContext(ioDispatcher) {
        sessionDao.byId(sessionId)?.let { Mappers.toSession(it, setLogDao.forSession(sessionId)) }
    }

    override suspend fun startSession(
        plan: TrainingPlan,
        day: PlanDay,
        startedAt: Long,
    ): WorkoutSession = withContext(ioDispatcher) {
        // Resume today's unfinished session rather than creating a duplicate.
        val existing = sessionDao.find(day.id, day.dateIso)
        if (existing != null) {
            return@withContext Mappers.toSession(existing, setLogDao.forSession(existing.id))
        }
        val entity = SessionEntity(
            id = "session-" + UUID.randomUUID(),
            planId = plan.id,
            dayId = day.id,
            weekIndex = day.weekIndex,
            dayIndex = day.dayOfWeek,
            dateIso = day.dateIso,
            title = day.title,
            status = SessionStatus.IN_PROGRESS.name,
            startedAt = startedAt,
            completedAt = null,
        )
        sessionDao.upsert(entity)
        Mappers.toSession(entity, emptyList())
    }

    override suspend fun logSet(sessionId: String, set: SessionSet): Long = withContext(ioDispatcher) {
        val stamped = if (set.completedAt > 0) set else set.copy(completedAt = timeProvider.nowMillis())
        setLogDao.insert(Mappers.toSetEntity(sessionId, stamped))
    }

    override suspend fun removeSet(sessionId: String, setRowId: Long) = withContext(ioDispatcher) {
        setLogDao.delete(setRowId)
    }

    override suspend fun completeSession(sessionId: String, completedAt: Long) = withContext(ioDispatcher) {
        sessionDao.setStatus(sessionId, SessionStatus.COMPLETED.name, completedAt)
    }

    override suspend fun abandonSession(sessionId: String, endedAt: Long) = withContext(ioDispatcher) {
        sessionDao.setStatus(sessionId, SessionStatus.ABANDONED.name, endedAt)
    }

    override suspend fun previousSets(exerciseId: String, beforeMillis: Long): List<SessionSet> =
        withContext(ioDispatcher) {
            // Only the most recent session counts as "last time" — earlier sessions are history.
            val rows = setLogDao.recentForExercise(exerciseId, beforeMillis)
            if (rows.isEmpty()) return@withContext emptyList()
            val latestSessionId = rows.first().sessionId
            rows.filter { it.sessionId == latestSessionId }.map(Mappers::toSessionSet)
        }

    override suspend fun hasCompletedSession(dayId: String, date: LocalDate): Boolean =
        withContext(ioDispatcher) {
            sessionDao.find(dayId, date.toString())?.status == SessionStatus.COMPLETED.name
        }

    override suspend fun clear() = withContext(ioDispatcher) {
        setLogDao.clear()
        sessionDao.clear()
    }
}
