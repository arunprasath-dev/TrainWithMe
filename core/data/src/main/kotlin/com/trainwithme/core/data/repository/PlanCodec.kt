package com.trainwithme.core.data.repository

import com.trainwithme.core.model.plan.TrainingPlan
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Serialises a plan to/from JSON. The payload is versioned ([TrainingPlan.schemaVersion]) so a
 * future release can migrate or regenerate instead of crashing on old data.
 */
object PlanCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(plan: TrainingPlan): String = json.encodeToString(TrainingPlan.serializer(), plan)

    fun decode(payload: String): TrainingPlan? = try {
        json.decodeFromString(TrainingPlan.serializer(), payload)
    } catch (t: Throwable) {
        Timber.e(t, "Unable to decode plan payload")
        null
    }
}
