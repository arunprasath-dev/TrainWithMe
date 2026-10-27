package com.trainwithme.core.model.plan

import kotlinx.serialization.Serializable

/** Machine-readable notes attached to a generated plan; the UI owns the wording. */
@Serializable
enum class PlanNoteCode {
    DELOAD_SCHEDULED,
    SESSION_TRUNCATED_FOR_TIME,
    EQUIPMENT_LIMITED_SELECTION,
    SAFETY_UNDERWEIGHT_FAT_LOSS,
    SAFETY_MINOR,
    AGGRESSIVE_RATE,
    HIGH_BODY_FAT,
    WAIST_RISK,
}

/**
 * @param args positional values referenced by the UI string, e.g. week numbers or percentages.
 */
@Serializable
data class PlanNote(
    val code: PlanNoteCode,
    val args: List<String> = emptyList(),
)
