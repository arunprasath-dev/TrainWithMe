package com.trainwithme.core.data.repository

import com.trainwithme.core.data.di.IoDispatcher
import com.trainwithme.core.data.local.db.MeasurementDao
import com.trainwithme.core.data.local.db.SetLogDao
import com.trainwithme.core.data.local.db.SessionDao
import com.trainwithme.core.model.progress.DatedSet
import com.trainwithme.core.model.progress.PersonalRecord
import com.trainwithme.core.model.progress.ProgressStats
import com.trainwithme.core.model.progress.WeekVolume
import com.trainwithme.core.model.progress.WeightPoint
import com.trainwithme.core.model.training.LoggedSet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultProgressRepository @Inject constructor(
    private val setLogDao: SetLogDao,
    private val measurementDao: MeasurementDao,
    private val sessionDao: SessionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProgressRepository {

    private fun zone(): ZoneId = ZoneId.systemDefault()

    private fun dateOf(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone()).toLocalDate()

    override fun observeWeightPoints(): Flow<List<WeightPoint>> =
        measurementDao.observeAll().map { rows ->
            rows.map { WeightPoint(dateOf(it.recordedAt), it.weightKg) }.sortedBy { it.date }
        }

    override fun observeBodyFatPoints(): Flow<List<WeightPoint>> =
        measurementDao.observeAll().map { rows ->
            rows.mapNotNull { row -> row.bodyFatPercent?.let { WeightPoint(dateOf(row.recordedAt), it) } }
                .sortedBy { it.date }
        }

    override fun observePersonalRecords(): Flow<List<PersonalRecord>> =
        setLogDao.observeHistory().map { rows ->
            ProgressStats.personalRecords(
                rows.map { row ->
                    DatedSet(
                        exerciseId = row.exerciseId,
                        date = LocalDate.parse(row.dateIso),
                        set = LoggedSet(
                            setIndex = row.setIndex,
                            weightKg = row.weightKg,
                            reps = row.reps,
                            rpe = row.rpe,
                            isWarmup = row.isWarmup,
                        ),
                    )
                },
            )
        }

    override fun observeWeeklyVolume(): Flow<List<WeekVolume>> =
        setLogDao.observeHistory().map { rows ->
            ProgressStats.weeklyVolume(
                rows.map { row ->
                    DatedSet(
                        exerciseId = row.exerciseId,
                        date = LocalDate.parse(row.dateIso),
                        set = LoggedSet(row.setIndex, row.weightKg, row.reps, row.rpe, row.isWarmup),
                    )
                },
            )
        }

    override fun observeHistory(): Flow<List<LoggedHistoryEntry>> =
        setLogDao.observeHistory().map { rows ->
            rows.map { row ->
                LoggedHistoryEntry(
                    exerciseId = row.exerciseId,
                    exerciseName = row.exerciseName,
                    date = LocalDate.parse(row.dateIso),
                    weightKg = row.weightKg,
                    reps = row.reps,
                    rpe = row.rpe,
                    isWarmup = row.isWarmup,
                )
            }
        }

    override fun observeCompletedCount(): Flow<Int> = sessionDao.observeCompletedCount()
}
