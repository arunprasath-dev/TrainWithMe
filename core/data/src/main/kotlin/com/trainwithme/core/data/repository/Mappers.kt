package com.trainwithme.core.data.repository

import com.trainwithme.core.data.local.db.MeasurementEntity
import com.trainwithme.core.data.local.db.ProfileEntity
import com.trainwithme.core.data.local.db.SessionEntity
import com.trainwithme.core.data.local.db.SetLogEntity
import com.trainwithme.core.model.profile.ActivityLevel
import com.trainwithme.core.model.profile.Circumference
import com.trainwithme.core.model.profile.EquipmentAccess
import com.trainwithme.core.model.profile.ExperienceLevel
import com.trainwithme.core.model.profile.Goal
import com.trainwithme.core.model.profile.Sex
import com.trainwithme.core.model.profile.UserProfile
import com.trainwithme.core.model.training.SessionSet
import com.trainwithme.core.model.training.SessionStatus
import com.trainwithme.core.model.training.WorkoutSession
import java.time.LocalDate

/** Maps between the persisted rows and the pure domain models. */
internal object Mappers {

    fun toDomain(entity: ProfileEntity): UserProfile = UserProfile(
        id = entity.id,
        name = entity.name,
        sex = enumValueOfOr(entity.sex, Sex.MALE),
        age = entity.age,
        heightCm = entity.heightCm,
        weightKg = entity.weightKg,
        bodyFatPercent = entity.bodyFatPercent,
        experience = enumValueOfOr(entity.experience, ExperienceLevel.BEGINNER),
        activityLevel = enumValueOfOr(entity.activityLevel, ActivityLevel.LIGHTLY_ACTIVE),
        goal = enumValueOfOr(entity.goal, Goal.BUILD_MUSCLE),
        equipmentAccess = enumValueOfOr(entity.equipmentAccess, EquipmentAccess.FULL_GYM),
        daysPerWeek = entity.daysPerWeek,
        sessionMinutes = entity.sessionMinutes,
        goalRateKgPerWeek = entity.goalRateKgPerWeek,
        circumferences = Circumference(
            neckCm = entity.neckCm,
            chestCm = entity.chestCm,
            waistCm = entity.waistCm,
            hipCm = entity.hipCm,
            armCm = entity.armCm,
            thighCm = entity.thighCm,
        ),
        agreedToDisclaimer = entity.agreedToDisclaimer,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    fun toEntity(profile: UserProfile): ProfileEntity = ProfileEntity(
        id = profile.id,
        name = profile.name,
        sex = profile.sex.name,
        age = profile.age,
        heightCm = profile.heightCm,
        weightKg = profile.weightKg,
        bodyFatPercent = profile.bodyFatPercent,
        experience = profile.experience.name,
        activityLevel = profile.activityLevel.name,
        goal = profile.goal.name,
        equipmentAccess = profile.equipmentAccess.name,
        daysPerWeek = profile.daysPerWeek,
        sessionMinutes = profile.sessionMinutes,
        goalRateKgPerWeek = profile.goalRateKgPerWeek,
        neckCm = profile.circumferences.neckCm,
        chestCm = profile.circumferences.chestCm,
        waistCm = profile.circumferences.waistCm,
        hipCm = profile.circumferences.hipCm,
        armCm = profile.circumferences.armCm,
        thighCm = profile.circumferences.thighCm,
        agreedToDisclaimer = profile.agreedToDisclaimer,
        createdAt = profile.createdAt,
        updatedAt = profile.updatedAt,
    )

    fun toSession(entity: SessionEntity, sets: List<SetLogEntity>): WorkoutSession = WorkoutSession(
        id = entity.id,
        planId = entity.planId,
        dayId = entity.dayId,
        weekIndex = entity.weekIndex,
        dayIndex = entity.dayIndex,
        date = LocalDate.parse(entity.dateIso),
        title = entity.title,
        status = enumValueOfOr(entity.status, SessionStatus.IN_PROGRESS),
        startedAt = entity.startedAt,
        completedAt = entity.completedAt,
        sets = sets.sortedWith(compareBy({ it.setIndex }, { it.id })).map(::toSessionSet),
    )

    fun toSessionSet(entity: SetLogEntity): SessionSet = SessionSet(
        id = entity.id,
        exerciseId = entity.exerciseId,
        exerciseName = entity.exerciseName,
        setIndex = entity.setIndex,
        weightKg = entity.weightKg,
        reps = entity.reps,
        rpe = entity.rpe,
        isWarmup = entity.isWarmup,
        targetReps = entity.targetReps,
        completedAt = entity.completedAt,
    )

    fun toSetEntity(sessionId: String, set: SessionSet): SetLogEntity = SetLogEntity(
        id = set.id,
        sessionId = sessionId,
        exerciseId = set.exerciseId,
        exerciseName = set.exerciseName,
        setIndex = set.setIndex,
        weightKg = set.weightKg,
        reps = set.reps,
        rpe = set.rpe,
        isWarmup = set.isWarmup,
        targetReps = set.targetReps,
        completedAt = set.completedAt,
    )

    fun toMeasurementEntity(profileId: Long, recordedAt: Long, weightKg: Double, bodyFatPercent: Double?, circumference: Circumference, note: String? = null): MeasurementEntity =
        MeasurementEntity(
            profileId = profileId,
            recordedAt = recordedAt,
            weightKg = weightKg,
            bodyFatPercent = bodyFatPercent,
            neckCm = circumference.neckCm,
            chestCm = circumference.chestCm,
            waistCm = circumference.waistCm,
            hipCm = circumference.hipCm,
            armCm = circumference.armCm,
            thighCm = circumference.thighCm,
            note = note,
        )

    fun circumferenceOf(entity: MeasurementEntity): Circumference = Circumference(
        neckCm = entity.neckCm,
        chestCm = entity.chestCm,
        waistCm = entity.waistCm,
        hipCm = entity.hipCm,
        armCm = entity.armCm,
        thighCm = entity.thighCm,
    )

    /** Tolerant enum parsing: a renamed member in a future release must not crash old data. */
    private inline fun <reified T : Enum<T>> enumValueOfOr(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)
}
