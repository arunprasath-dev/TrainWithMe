package com.agon.app.data.repository

import com.agon.app.data.local.MeasurementDao
import com.agon.app.data.local.MeasurementEntity
import com.agon.app.data.local.ProfileDao
import com.agon.app.data.util.TimeProvider
import com.agon.app.domain.model.profile.Circumference
import com.agon.app.domain.model.profile.UserProfile
import com.agon.app.domain.model.progress.WeightPoint
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class DefaultProfileRepository (
    private val profileDao: ProfileDao,
    private val timeProvider: TimeProvider,
) : ProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        profileDao.observe().map { entity -> entity?.let(Mappers::toDomain) }

    override suspend fun getProfile(): UserProfile? = profileDao.get()?.let(Mappers::toDomain)

    override suspend fun saveProfile(profile: UserProfile) {
        val now = timeProvider.nowMillis()
        val existing = profileDao.get()
        val withTimestamps = profile.copy(
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        profileDao.upsert(Mappers.toEntity(withTimestamps))
    }

    override suspend fun clear() = profileDao.clear()
}


class DefaultMeasurementRepository (
    private val measurementDao: MeasurementDao,
    private val timeProvider: TimeProvider,
) : MeasurementRepository {

    override fun observeAll(): Flow<List<BodyMeasurement>> =
        measurementDao.observeAll().map { rows -> rows.map(::toDomain) }

    override fun observeWeightSeries(): Flow<List<WeightPoint>> =
        measurementDao.observeAll().map { rows -> rows.map(::toWeightPoint) }

    override fun observeBodyFatSeries(): Flow<List<WeightPoint>> =
        measurementDao.observeAll().map { rows ->
            rows.mapNotNull { row ->
                row.bodyFatPercent?.let { WeightPoint(Instant.ofEpochMilli(row.recordedAt).atZone(zone()).toLocalDate(), it) }
            }
        }

    override suspend fun latest(): BodyMeasurement? = measurementDao.latest()?.let(::toDomain)

    override suspend fun add(
        weightKg: Double,
        bodyFatPercent: Double?,
        circumference: Circumference,
        note: String?,
        recordedAt: Long,
    ) {
        val profileId = UserProfile.SINGLE_PROFILE_ID
        measurementDao.insert(
            Mappers.toMeasurementEntity(profileId, recordedAt, weightKg, bodyFatPercent, circumference, note),
        )
    }

    override suspend fun delete(id: Long) = measurementDao.delete(id)

    override suspend fun clear() = measurementDao.clear()

    private fun toDomain(entity: MeasurementEntity) = BodyMeasurement(
        id = entity.id,
        recordedAt = entity.recordedAt,
        weightKg = entity.weightKg,
        bodyFatPercent = entity.bodyFatPercent,
        circumference = Mappers.circumferenceOf(entity),
        note = entity.note,
    )

    private fun toWeightPoint(entity: MeasurementEntity) = WeightPoint(
        date = Instant.ofEpochMilli(entity.recordedAt).atZone(zone()).toLocalDate(),
        weightKg = entity.weightKg,
    )

    private fun zone(): ZoneId = timeProvider.zone()
}
