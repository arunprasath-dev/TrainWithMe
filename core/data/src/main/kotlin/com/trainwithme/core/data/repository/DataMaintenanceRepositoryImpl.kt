package com.trainwithme.core.data.repository

import com.trainwithme.core.data.di.IoDispatcher
import com.trainwithme.core.data.local.db.MeasurementDao
import com.trainwithme.core.data.local.db.PlanDao
import com.trainwithme.core.data.local.db.ProfileDao
import com.trainwithme.core.data.local.db.SessionDao
import com.trainwithme.core.data.local.db.SetLogDao
import com.trainwithme.core.data.util.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Export and delete-all. The export is a plain JSON document the user owns (docs/PLAN.md §6.7 G4):
 * no cloud, no account, no proprietary format.
 */
@Singleton
class DefaultDataMaintenanceRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val measurementDao: MeasurementDao,
    private val planDao: PlanDao,
    private val sessionDao: SessionDao,
    private val setLogDao: SetLogDao,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DataMaintenanceRepository {

    private val json = Json { prettyPrint = true }

    override suspend fun exportJson(): String = withContext(ioDispatcher) {
        val profile = profileDao.get()
        val measurements = measurementDao.getAll()
        val plans = planDao.getAll()
        val sessions = sessionDao.getAll()

        val document = buildJsonObject {
            put("exportedAtMillis", timeProvider.nowMillis())
            put("exportSchema", EXPORT_SCHEMA)
            putJsonObject("profile") {
                if (profile == null) {
                    put("present", false)
                } else {
                    put("present", true)
                    put("name", profile.name)
                    put("sex", profile.sex)
                    put("age", profile.age)
                    put("heightCm", profile.heightCm)
                    put("weightKg", profile.weightKg)
                    put("bodyFatPercent", profile.bodyFatPercent)
                    put("experience", profile.experience)
                    put("activityLevel", profile.activityLevel)
                    put("goal", profile.goal)
                    put("equipmentAccess", profile.equipmentAccess)
                    put("daysPerWeek", profile.daysPerWeek)
                    put("sessionMinutes", profile.sessionMinutes)
                }
            }
            putJsonArray("measurements") {
                measurements.forEach { row ->
                    add(
                        buildJsonObject {
                            put("recordedAtMillis", row.recordedAt)
                            put("weightKg", row.weightKg)
                            put("bodyFatPercent", row.bodyFatPercent)
                            put("neckCm", row.neckCm)
                            put("chestCm", row.chestCm)
                            put("waistCm", row.waistCm)
                            put("hipCm", row.hipCm)
                            put("armCm", row.armCm)
                            put("thighCm", row.thighCm)
                            put("note", row.note)
                        },
                    )
                }
            }
            putJsonArray("plans") {
                plans.forEach { plan ->
                    add(
                        buildJsonObject {
                            put("id", plan.id)
                            put("createdAtMillis", plan.createdAt)
                            put("startDate", plan.startDateIso)
                            put("splitName", plan.splitName)
                            put("isActive", plan.isActive)
                            put("schemaVersion", plan.schemaVersion)
                            // The generated programme itself, so the export is self-contained.
                            put("payload", plan.payloadJson)
                        },
                    )
                }
            }
            putJsonArray("sessions") {
                sessions.forEach { session ->
                    add(
                        buildJsonObject {
                            put("id", session.id)
                            put("planId", session.planId)
                            put("dayId", session.dayId)
                            put("date", session.dateIso)
                            put("title", session.title)
                            put("status", session.status)
                            put("startedAtMillis", session.startedAt)
                            put("completedAtMillis", session.completedAt)
                            putJsonArray("sets") {
                                setLogDao.forSession(session.id).forEach { set ->
                                    add(
                                        buildJsonObject {
                                            put("exerciseId", set.exerciseId)
                                            put("exerciseName", set.exerciseName)
                                            put("setIndex", set.setIndex)
                                            put("weightKg", set.weightKg)
                                            put("reps", set.reps)
                                            put("rpe", set.rpe)
                                            put("isWarmup", set.isWarmup)
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
        json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), document)
    }

    override suspend fun deleteAllData() = withContext(ioDispatcher) {
        // Children before parents so foreign keys never block the wipe.
        setLogDao.clear()
        sessionDao.clear()
        planDao.clear()
        measurementDao.clear()
        profileDao.clear()
    }

    private companion object {
        const val EXPORT_SCHEMA = 1
    }
}
