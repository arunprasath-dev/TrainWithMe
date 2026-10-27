package com.trainwithme.core.data.repository

import com.trainwithme.core.data.asset.AssetExerciseLibrary
import com.trainwithme.core.data.local.db.PlanDao
import com.trainwithme.core.data.local.db.PlanEntity
import com.trainwithme.core.data.util.TimeProvider
import com.trainwithme.core.model.plan.PlanGenerationRequest
import com.trainwithme.core.model.plan.PlanGenerationResult
import com.trainwithme.core.model.plan.PlanGenerator
import com.trainwithme.core.model.plan.TrainingPlan
import com.trainwithme.core.model.profile.UserProfile
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.trainwithme.core.data.di.IoDispatcher
import timber.log.Timber

@Singleton
class DefaultPlanRepository @Inject constructor(
    private val planDao: PlanDao,
    private val exerciseLibrary: AssetExerciseLibrary,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PlanRepository {

    private val generator = PlanGenerator(exerciseLibrary, clock = timeProvider::nowMillis)

    override fun observeActivePlan(): Flow<TrainingPlan?> =
        planDao.observeActive().map { entity -> entity?.payloadJson?.let(PlanCodec::decode) }

    override suspend fun activePlan(): TrainingPlan? =
        withContext(ioDispatcher) { planDao.active()?.payloadJson?.let(PlanCodec::decode) }

    override suspend fun generatePlan(profile: UserProfile, startDate: LocalDate): PlanGenerationResult =
        withContext(ioDispatcher) {
            val request = PlanGenerationRequest(
                profile = profile,
                startDate = startDate,
                planId = "plan-" + UUID.randomUUID(),
            )
            when (val result = generator.generate(request)) {
                is PlanGenerationResult.Invalid -> result
                is PlanGenerationResult.Success -> {
                    val plan = result.plan
                    planDao.activate(
                        PlanEntity(
                            id = plan.id,
                            createdAt = plan.createdAtMillis,
                            startDateIso = plan.startDateIso,
                            splitName = plan.splitName,
                            weeks = plan.weekCount,
                            isActive = true,
                            schemaVersion = plan.schemaVersion,
                            payloadJson = PlanCodec.encode(plan),
                        ),
                    )
                    Timber.i("Activated plan %s (%s)", plan.id, plan.splitName)
                    result
                }
            }
        }

    override suspend fun archiveActivePlan() = withContext(ioDispatcher) { planDao.deactivateAll() }
}
