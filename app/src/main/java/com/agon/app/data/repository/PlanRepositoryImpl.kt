package com.agon.app.data.repository

import com.agon.app.data.asset.AssetExerciseLibrary
import com.agon.app.data.local.PlanDao
import com.agon.app.data.local.PlanEntity
import com.agon.app.data.util.TimeProvider
import com.agon.app.domain.model.plan.PlanGenerationRequest
import com.agon.app.domain.model.plan.PlanGenerationResult
import com.agon.app.domain.model.plan.PlanGenerator
import com.agon.app.domain.model.plan.TrainingPlan
import com.agon.app.domain.model.profile.UserProfile
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


class DefaultPlanRepository (
    private val planDao: PlanDao,
    private val exerciseLibrary: AssetExerciseLibrary,
    private val timeProvider: TimeProvider,
    private val ioDispatcher: CoroutineDispatcher,
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
                    android.util.Log.i("PlanRepository", "Activated plan ${plan.id} (${plan.splitName})")
                    result
                }
            }
        }

    override suspend fun archiveActivePlan() = withContext(ioDispatcher) { planDao.deactivateAll() }
}
