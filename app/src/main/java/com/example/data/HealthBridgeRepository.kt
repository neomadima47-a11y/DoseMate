package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class HealthBridgeRepository(
    private val userDao: UserDao,
    private val medicationDao: MedicationDao,
    private val healthReadingDao: HealthReadingDao,
    private val clinicDao: ClinicDao
) {
    val userSession: Flow<UserSessionEntity?> = userDao.getUserSession()
    val medications: Flow<List<MedicationEntity>> = medicationDao.getAllMedications()
    val doseLogs: Flow<List<DoseLogEntity>> = medicationDao.getAllDoseLogs()
    val healthReadings: Flow<List<HealthReadingEntity>> = healthReadingDao.getAllReadings()
    val pendingSyncCount: Flow<Int> = healthReadingDao.getPendingCount()
    val clinics: Flow<List<ClinicEntity>> = clinicDao.getAllClinics()

    suspend fun initializeDefaultDataIfEmpty() {
        // Initialize User Session
        val user = userDao.getUserSession().first()
        if (user == null) {
            userDao.insertOrUpdateUser(
                UserSessionEntity(
                    fullName = "Lerato Mokoena",
                    email = "lerato@example.com",
                    preferredLanguage = "English",
                    isLoggedIn = true,
                    medicationRemindersEnabled = true,
                    syncAlertsEnabled = true,
                    darkThemeEnabled = true
                )
            )
        }

        // Initialize Default Medications
        val currentMeds = medicationDao.getAllMedications().first()
        if (currentMeds.isEmpty()) {
            val metId = medicationDao.insertMedication(
                MedicationEntity(
                    name = "Metformin",
                    dosage = "500mg",
                    frequency = "Twice daily",
                    reminderTimes = "7:00 AM, 7:00 PM",
                    status = "Active"
                )
            )
            val amlId = medicationDao.insertMedication(
                MedicationEntity(
                    name = "Amlodipine",
                    dosage = "5mg",
                    frequency = "Once daily",
                    reminderTimes = "6:00 PM",
                    status = "Active"
                )
            )
            medicationDao.insertMedication(
                MedicationEntity(
                    name = "Amoxicillin",
                    dosage = "250mg",
                    frequency = "3x daily",
                    reminderTimes = "8:00 AM, 2:00 PM, 8:00 PM",
                    status = "Ended"
                )
            )

            // Insert dose logs for today
            medicationDao.insertDoseLogs(
                listOf(
                    DoseLogEntity(
                        medicationId = metId,
                        medicationName = "Metformin 500mg",
                        scheduledTime = "7:00am",
                        takenTime = "7:05am",
                        status = "Taken",
                        date = "Today"
                    ),
                    DoseLogEntity(
                        medicationId = amlId,
                        medicationName = "Amlodipine 5mg",
                        scheduledTime = "6:00pm",
                        takenTime = null,
                        status = "Pending",
                        date = "Today"
                    )
                )
            )
        }

        // Initialize Default Health Readings
        val currentReadings = healthReadingDao.getAllReadings().first()
        if (currentReadings.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            healthReadingDao.insertReading(
                HealthReadingEntity(
                    type = "Blood sugar",
                    value = "7.2",
                    unit = "mmol/L",
                    whenTaken = "Before breakfast",
                    notes = "Fasting reading before morning tea",
                    timestamp = now - 18000000L,
                    displayTime = "Today, 6:45am",
                    isPendingSync = true
                )
            )
            healthReadingDao.insertReading(
                HealthReadingEntity(
                    type = "Blood sugar",
                    value = "8.9",
                    unit = "mmol/L",
                    whenTaken = "After dinner",
                    notes = "Felt slightly tired after meal",
                    timestamp = now - dayMs,
                    displayTime = "Yesterday, 8:15pm",
                    isPendingSync = true
                )
            )
            healthReadingDao.insertReading(
                HealthReadingEntity(
                    type = "Blood sugar",
                    value = "6.8",
                    unit = "mmol/L",
                    whenTaken = "Before breakfast",
                    notes = "Good resting level",
                    timestamp = now - (2 * dayMs),
                    displayTime = "2 days ago",
                    isPendingSync = false
                )
            )
            healthReadingDao.insertReading(
                HealthReadingEntity(
                    type = "Blood pressure",
                    value = "120/80",
                    unit = "mmHg",
                    whenTaken = "Resting",
                    notes = "Normal resting pressure",
                    timestamp = now - (3 * dayMs),
                    displayTime = "3 days ago",
                    isPendingSync = false
                )
            )
        }

        // Initialize Clinics
        val currentClinics = clinicDao.getAllClinics().first()
        if (currentClinics.isEmpty()) {
            clinicDao.insertClinics(
                listOf(
                    ClinicEntity(
                        name = "Sunnyside Community Clinic",
                        distanceKm = 1.2,
                        openHours = "open until 6pm",
                        address = "45 Kotze St, Sunnyside, Pretoria",
                        phone = "+27 12 555 0192",
                        isNearest = true,
                        latOffsetDp = -20f,
                        lngOffsetDp = 10f
                    ),
                    ClinicEntity(
                        name = "Riverside Clinic",
                        distanceKm = 2.8,
                        openHours = "open until 4pm",
                        address = "128 River Road, Johannesburg",
                        phone = "+27 11 442 9801",
                        isNearest = false,
                        latOffsetDp = 30f,
                        lngOffsetDp = -40f
                    ),
                    ClinicEntity(
                        name = "Hillbrow Health Centre",
                        distanceKm = 4.5,
                        openHours = "open until 5pm",
                        address = "82 Smit St, Hillbrow, Johannesburg",
                        phone = "+27 11 720 3300",
                        isNearest = false,
                        latOffsetDp = 60f,
                        lngOffsetDp = 50f
                    )
                )
            )
        }
    }

    suspend fun updateUserProfile(
        fullName: String,
        email: String,
        preferredLanguage: String,
        reminders: Boolean,
        darkTheme: Boolean
    ) {
        val current = userDao.getUserSession().first() ?: UserSessionEntity()
        userDao.insertOrUpdateUser(
            current.copy(
                fullName = fullName,
                email = email,
                preferredLanguage = preferredLanguage,
                medicationRemindersEnabled = reminders,
                darkThemeEnabled = darkTheme
            )
        )
    }

    suspend fun login(email: String, name: String, language: String) {
        val current = userDao.getUserSession().first() ?: UserSessionEntity()
        userDao.insertOrUpdateUser(
            current.copy(
                email = email,
                fullName = if (name.isNotBlank()) name else "Patient",
                preferredLanguage = language,
                isLoggedIn = true
            )
        )
    }

    suspend fun logout() {
        val current = userDao.getUserSession().first()
        if (current != null) {
            userDao.insertOrUpdateUser(current.copy(isLoggedIn = false))
        }
    }

    suspend fun addMedication(name: String, dosage: String, frequency: String, reminderTimes: String) {
        val medId = medicationDao.insertMedication(
            MedicationEntity(
                name = name,
                dosage = dosage,
                frequency = frequency,
                reminderTimes = reminderTimes,
                status = "Active"
            )
        )
        // Add pending dose log
        medicationDao.insertDoseLogs(
            listOf(
                DoseLogEntity(
                    medicationId = medId,
                    medicationName = "$name $dosage",
                    scheduledTime = reminderTimes.split(",").firstOrNull()?.trim() ?: "8:00 AM",
                    status = "Pending",
                    date = "Today"
                )
            )
        )
    }

    suspend fun markDoseTaken(doseLogId: Long, timeStr: String) {
        medicationDao.updateDoseStatus(doseLogId, "Taken", timeStr)
    }

    suspend fun deleteMedication(id: Long) {
        medicationDao.deleteMedication(id)
        medicationDao.deleteDoseLogsForMedication(id)
    }

    suspend fun updateMedicationStatus(id: Long, status: String) {
        medicationDao.updateMedicationStatus(id, status)
    }

    suspend fun addHealthReading(
        type: String,
        value: String,
        unit: String,
        whenTaken: String,
        notes: String
    ) {
        val now = java.util.Calendar.getInstance()
        val timeFormat = java.text.SimpleDateFormat("h:mma", java.util.Locale.US)
        val formattedTime = "Today, ${timeFormat.format(now.time).lowercase()}"

        healthReadingDao.insertReading(
            HealthReadingEntity(
                type = type,
                value = value,
                unit = unit,
                whenTaken = whenTaken,
                notes = notes,
                timestamp = System.currentTimeMillis(),
                displayTime = formattedTime,
                isPendingSync = true
            )
        )
    }

    suspend fun deleteHealthReading(id: Long) {
        healthReadingDao.deleteReading(id)
    }

    suspend fun addClinics(newClinics: List<ClinicEntity>) {
        clinicDao.insertClinics(newClinics)
    }

    suspend fun syncOfflineData() {
        healthReadingDao.markAllSynced()
    }
}
