package com.agon.app.data.repository

import com.agon.app.data.local.MeasurementDao
import com.agon.app.data.local.PlanDao
import com.agon.app.data.local.ProfileDao
import com.agon.app.data.local.SessionDao
import com.agon.app.data.local.SetLogDao
import com.agon.app.data.util.TimeProvider
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

class DefaultDataMaintenanceRepository (
    private val profileDao: ProfileDao,
    private val measurementDao: MeasurementDao,
    private val planDao: PlanDao,
    private val sessionDao: SessionDao,
    private val setLogDao: SetLogDao,
    private val timeProvider: TimeProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : DataMaintenanceRepository {

    private val json = Json { prettyPrint = true }

    override suspend fun exportJson(): String = withContext(ioDispatcher) {
        val profile = profileDao.get()
        val measurements = measurementDao.getAll()
        val plans = planDao.getAll()
        val sessions = sessionDao.getAll()
        // Suspend DAO reads must complete before the (non-suspend) JSON builders run.
        val setsBySession = sessions.associate { session -> session.id to setLogDao.forSession(session.id) }

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
                                setsBySession[session.id].orEmpty().forEach { set ->
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
