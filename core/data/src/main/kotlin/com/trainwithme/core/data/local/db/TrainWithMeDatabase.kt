package com.trainwithme.core.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database. Schema exports live in `core/data/schemas` and are committed — every released
 * version bump adds an explicit migration (docs/PLAN.md §9.6).
 */
@Database(
    entities = [
        ProfileEntity::class,
        MeasurementEntity::class,
        PlanEntity::class,
        SessionEntity::class,
        SetLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TrainWithMeDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao
    abstract fun setLogDao(): SetLogDao

    companion object {
        const val NAME = "trainwithme.db"

        fun build(context: Context): TrainWithMeDatabase =
            Room.databaseBuilder(context.applicationContext, TrainWithMeDatabase::class.java, NAME)
                // Foreign keys are declared on entities; SQLite needs them switched on per connection.
                .addCallback(
                    object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    },
                )
                .build()
    }
}

// Note: every column is a primitive (String/Double/Int/Long/Boolean), so no TypeConverters are
// needed. Domain enums are stored by name and timestamps as epoch millis, which keeps the schema
// stable when enum members are added later.
