package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.HealthBridgeDatabase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getLong("medication_id", -1L)
        val medicationName = inputData.getString("medication_name") ?: "Medication"
        val dosage = inputData.getString("dosage") ?: ""
        val scheduledTime = inputData.getString("scheduled_time") ?: ""

        if (medicationId == -1L) return Result.failure()

        try {
            val database = HealthBridgeDatabase.getDatabase(context)
            val dao = database.medicationDao()

            // Verify the medication is still active
            val allMeds = dao.getAllMedications().first()
            val activeMed = allMeds.find { it.id == medicationId && it.status == "Active" }

            if (activeMed != null) {
                // Check if dose has already been taken today
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val todayStr = dateFormat.format(Date())
                val logs = dao.getAllDoseLogs().first()

                val alreadyTaken = logs.any { log ->
                    log.medicationId == medicationId &&
                        (log.date == "Today" || log.date == todayStr) &&
                        log.status == "Taken" &&
                        log.scheduledTime.equals(scheduledTime, ignoreCase = true)
                }

                if (!alreadyTaken) {
                    MedicationNotificationHelper.showMedicationReminder(
                        context = context,
                        medicationId = medicationId,
                        medicationName = medicationName,
                        dosage = dosage,
                        scheduledTime = scheduledTime
                    )
                }
            }

            // Reschedule for tomorrow at the same scheduled time
            MedicationNotificationScheduler.scheduleNextDailyOccurrence(
                context = context,
                medicationId = medicationId,
                medicationName = medicationName,
                dosage = dosage,
                scheduledTime = scheduledTime
            )

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
