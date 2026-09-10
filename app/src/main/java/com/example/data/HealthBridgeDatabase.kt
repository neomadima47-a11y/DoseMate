package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserSessionEntity::class,
        MedicationEntity::class,
        DoseLogEntity::class,
        HealthReadingEntity::class,
        ClinicEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HealthBridgeDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun medicationDao(): MedicationDao
    abstract fun healthReadingDao(): HealthReadingDao
    abstract fun clinicDao(): ClinicDao

    companion object {
        @Volatile
        private var INSTANCE: HealthBridgeDatabase? = null

        fun getDatabase(context: Context): HealthBridgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthBridgeDatabase::class.java,
                    "healthbridge_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
