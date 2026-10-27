package com.trainwithme.core.data.repository

import com.trainwithme.core.data.local.db.MeasurementDao
import com.trainwithme.core.data.local.db.MeasurementEntity
import com.trainwithme.core.data.local.db.ProfileDao
import com.trainwithme.core.data.util.TimeProvider
import com.trainwithme.core.model.profile.Circumference
import com.trainwithme.core.model.profile.UserProfile
import com.trainwithme.core.model.progress.WeightPoint
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultProfileRepository @Inject constructor(
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

@Singleton
class DefaultMeasurementRepository @Inject constructor(
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
