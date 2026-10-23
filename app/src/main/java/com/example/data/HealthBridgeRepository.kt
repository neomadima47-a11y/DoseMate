package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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
        // Initialize Default User Session
        val currentUser = userDao.getUserSession().first()
        if (currentUser == null) {
            userDao.insertOrUpdateUser(
                UserSessionEntity(
                    fullName = "Lerato Mokoena",
                    email = "lerato@example.com",
                    preferredLanguage = "English",
                    isLoggedIn = true
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
        val updatedUser = current.copy(
            email = email,
            fullName = if (name.isNotBlank()) name else "Patient",
            preferredLanguage = language,
            isLoggedIn = true
        )
        userDao.insertOrUpdateUser(updatedUser)

        val docId = email.replace(".", "_")
        val userMap = mapOf(
            "email" to email,
            "fullName" to updatedUser.fullName,
            "preferredLanguage" to language,
            "lastLoginTimestamp" to System.currentTimeMillis(),
            "condition" to updatedUser.condition
        )

        FirestoreClient.getUsersCollection().document(docId).set(userMap)
            .addOnSuccessListener {
                Log.d("FirestoreSync", "Successfully synced user profile for $email to Firestore!")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "Failed to sync user profile to Firestore (Check Security Rules): ${e.message}", e)
            }

        syncOfflineData()
    }

    suspend fun loginWithPin(email: String, name: String, language: String, pin: String) {
        val current = userDao.getUserSession().first() ?: UserSessionEntity()
        val updatedUser = current.copy(
            email = email,
            fullName = if (name.isNotBlank()) name else "Patient",
            preferredLanguage = language,
            securityPin = pin,
            isLoggedIn = true
        )
        userDao.insertOrUpdateUser(updatedUser)

        val docId = email.replace(".", "_")
        val userMap = mapOf(
            "email" to email,
            "fullName" to updatedUser.fullName,
            "preferredLanguage" to language,
            "securityPin" to pin,
            "lastLoginTimestamp" to System.currentTimeMillis(),
            "condition" to updatedUser.condition
        )

        FirestoreClient.getUsersCollection().document(docId).set(userMap)
            .addOnSuccessListener {
                Log.d("FirestoreSync", "Successfully synced user profile for $email to Firestore!")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "Failed to sync user profile to Firestore (Check Security Rules): ${e.message}", e)
            }

        syncOfflineData()
    }

    suspend fun updateSecurityPin(email: String, newPin: String) {
        val current = userDao.getUserSession().first() ?: UserSessionEntity()
        userDao.insertOrUpdateUser(
            current.copy(
                email = email,
                securityPin = newPin
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
        val medication = MedicationEntity(
            name = name,
            dosage = dosage,
            frequency = frequency,
            reminderTimes = reminderTimes,
            status = "Active",
            isPendingSync = true
        )
        val medId = medicationDao.insertMedication(medication)

        // Split reminder times and create a DoseLogEntity for EACH scheduled time
        val times = reminderTimes.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("08:00 AM") }

        val displayName = if (dosage.isNotBlank() && dosage != "1 dose" && dosage != "Standard dose") {
            "$name ($dosage)"
        } else {
            name
        }

        val doseLogs = times.map { timeStr ->
            DoseLogEntity(
                medicationId = medId,
                medicationName = displayName,
                scheduledTime = timeStr,
                status = "Pending",
                date = "Today",
                isPendingSync = true
            )
        }
        medicationDao.insertDoseLogs(doseLogs)

        try {
            val user = userDao.getUserSession().first()
            val userEmail = (user?.email ?: "anonymous").replace(".", "_")

            FirestoreClient.getMedicationsCollection()
                .document("${userEmail}_med_${medication.createdTimestamp}")
                .set(medication.copy(id = medId))
                .addOnSuccessListener { Log.d("FirestoreSync", "Medication synced to Firestore!") }
                .addOnFailureListener { e -> Log.e("FirestoreSync", "Medication sync failed: ${e.message}", e) }
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Live medication sync error: ${e.message}")
        }
    }

    suspend fun markDoseTaken(doseLogId: Long, timeStr: String) {
        medicationDao.updateDoseStatus(doseLogId, "Taken", timeStr)
        
        try {
            val allLogs = medicationDao.getAllDoseLogs().first()
            val log = allLogs.find { it.id == doseLogId }
            if (log != null) {
                val user = userDao.getUserSession().first()
                val userEmail = (user?.email ?: "anonymous").replace(".", "_")
                
                FirestoreClient.getDoseLogsCollection()
                    .document("${userEmail}_log_${log.medicationId}_${log.date}_${log.scheduledTime.hashCode()}")
                    .set(log.copy(isPendingSync = false))
                    .addOnSuccessListener { Log.d("FirestoreSync", "Dose log synced to Firestore!") }
                    .addOnFailureListener { e -> Log.e("FirestoreSync", "Dose log sync failed: ${e.message}", e) }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Dose log sync error: ${e.message}")
        }
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
        val timestamp = System.currentTimeMillis()

        val reading = HealthReadingEntity(
            type = type,
            value = value,
            unit = unit,
            whenTaken = whenTaken,
            notes = notes,
            timestamp = timestamp,
            displayTime = formattedTime,
            isPendingSync = true
        )

        healthReadingDao.insertReading(reading)

        try {
            val user = userDao.getUserSession().first()
            val userEmail = (user?.email ?: "anonymous").replace(".", "_")
            
            FirestoreClient.getReadingsCollection()
                .document("${userEmail}_reading_$timestamp")
                .set(reading)
                .addOnSuccessListener { Log.d("FirestoreSync", "Health reading synced to Firestore!") }
                .addOnFailureListener { e -> Log.e("FirestoreSync", "Health reading sync failed: ${e.message}", e) }
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Health reading sync error: ${e.message}")
        }
    }

    suspend fun deleteHealthReading(id: Long) {
        healthReadingDao.deleteReading(id)
    }

    suspend fun addClinics(newClinics: List<ClinicEntity>) {
        clinicDao.insertClinics(newClinics)
    }

    suspend fun syncOfflineData() {
        val user = userDao.getUserSession().first()
        val userEmail = (user?.email ?: "anonymous").replace(".", "_")

        // 1. Sync Health Readings
        val pendingReadings = healthReadingDao.getPendingSyncReadings().first()
        if (pendingReadings.isNotEmpty()) {
            pendingReadings.forEach { reading ->
                try {
                    FirestoreClient.getReadingsCollection()
                        .document("${userEmail}_reading_${reading.timestamp}")
                        .set(reading.copy(isPendingSync = false))
                        .addOnSuccessListener { Log.d("FirestoreSync", "Reading ${reading.id} synced!") }
                        .addOnFailureListener { e -> Log.e("FirestoreSync", "Reading sync failed: ${e.message}", e) }
                } catch (e: Exception) {
                    Log.e("FirestoreSync", "Reading sync exception: ${e.message}")
                }
            }
            healthReadingDao.markAllSynced()
        }

        // 2. Sync Medications
        val pendingMeds = medicationDao.getPendingSyncMedications()
        if (pendingMeds.isNotEmpty()) {
            pendingMeds.forEach { med ->
                try {
                    FirestoreClient.getMedicationsCollection()
                        .document("${userEmail}_med_${med.createdTimestamp}")
                        .set(med.copy(isPendingSync = false))
                        .addOnSuccessListener { Log.d("FirestoreSync", "Medication ${med.id} synced!") }
                        .addOnFailureListener { e -> Log.e("FirestoreSync", "Medication sync failed: ${e.message}", e) }
                } catch (e: Exception) {
                    Log.e("FirestoreSync", "Medication sync exception: ${e.message}")
                }
            }
            medicationDao.markMedicationsSynced()
        }

        // 3. Sync Dose Logs
        val pendingLogs = medicationDao.getPendingSyncDoseLogs()
        if (pendingLogs.isNotEmpty()) {
            pendingLogs.forEach { log ->
                try {
                    FirestoreClient.getDoseLogsCollection()
                        .document("${userEmail}_log_${log.medicationId}_${log.date}_${log.scheduledTime.hashCode()}")
                        .set(log.copy(isPendingSync = false))
                        .addOnSuccessListener { Log.d("FirestoreSync", "Dose log ${log.id} synced!") }
                        .addOnFailureListener { e -> Log.e("FirestoreSync", "Dose log sync failed: ${e.message}", e) }
                } catch (e: Exception) {
                    Log.e("FirestoreSync", "Dose log sync exception: ${e.message}")
                }
            }
            medicationDao.markDoseLogsSynced()
        }
    }
}
