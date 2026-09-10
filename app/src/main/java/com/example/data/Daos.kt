package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    fun getUserSession(): Flow<UserSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(userSession: UserSessionEntity)

    @Update
    suspend fun updateUser(userSession: UserSessionEntity)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY createdTimestamp DESC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Query("SELECT * FROM dose_logs WHERE date = :date")
    fun getDoseLogsForDate(date: String): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs")
    fun getAllDoseLogs(): Flow<List<DoseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLogs(logs: List<DoseLogEntity>)

    @Query("UPDATE dose_logs SET status = :status, takenTime = :takenTime WHERE id = :id")
    suspend fun updateDoseStatus(id: Long, status: String, takenTime: String)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: Long)

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId")
    suspend fun deleteDoseLogsForMedication(medicationId: Long)

    @Query("UPDATE medications SET status = :status WHERE id = :id")
    suspend fun updateMedicationStatus(id: Long, status: String)
}

@Dao
interface HealthReadingDao {
    @Query("SELECT * FROM health_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<HealthReadingEntity>>

    @Query("SELECT * FROM health_readings WHERE isPendingSync = 1")
    fun getPendingSyncReadings(): Flow<List<HealthReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: HealthReadingEntity): Long

    @Query("UPDATE health_readings SET isPendingSync = 0 WHERE isPendingSync = 1")
    suspend fun markAllSynced()

    @Query("SELECT COUNT(*) FROM health_readings WHERE isPendingSync = 1")
    fun getPendingCount(): Flow<Int>

    @Query("DELETE FROM health_readings WHERE id = :id")
    suspend fun deleteReading(id: Long)
}

@Dao
interface ClinicDao {
    @Query("SELECT * FROM clinics ORDER BY distanceKm ASC")
    fun getAllClinics(): Flow<List<ClinicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinics(clinics: List<ClinicEntity>)
}
