package com.example.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.MedicationEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object MedicationNotificationScheduler {

    const val TAG_MEDICATION_REMINDER = "medication_reminder"

    fun scheduleAllActiveMedications(context: Context, medications: List<MedicationEntity>) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TAG_MEDICATION_REMINDER)

        val activeMeds = medications.filter { it.status.equals("Active", ignoreCase = true) }
        for (med in activeMeds) {
            scheduleMedicationAlerts(context, med)
        }
    }

    fun scheduleMedicationAlerts(context: Context, medication: MedicationEntity) {
        if (!medication.status.equals("Active", ignoreCase = true)) return

        val timesList = medication.reminderTimes
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (timeStr in timesList) {
            scheduleSingleDoseTime(
                context = context,
                medicationId = medication.id,
                medicationName = medication.name,
                dosage = medication.dosage,
                scheduledTime = timeStr
            )
        }
    }

    private fun scheduleSingleDoseTime(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        scheduledTime: String
    ) {
        val delayMs = calculateInitialDelay(scheduledTime)
        val workTag = "med_${medicationId}_${scheduledTime.replace(" ", "_").replace(":", "_")}"

        val inputData = Data.Builder()
            .putLong("medication_id", medicationId)
            .putString("medication_name", medicationName)
            .putString("dosage", dosage)
            .putString("scheduled_time", scheduledTime)
            .build()

        val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(TAG_MEDICATION_REMINDER)
            .addTag(workTag)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workTag,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleNextDailyOccurrence(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        scheduledTime: String
    ) {
        val delayMs = 24L * 60L * 60L * 1000L
        val workTag = "med_${medicationId}_${scheduledTime.replace(" ", "_").replace(":", "_")}"

        val inputData = Data.Builder()
            .putLong("medication_id", medicationId)
            .putString("medication_name", medicationName)
            .putString("dosage", dosage)
            .putString("scheduled_time", scheduledTime)
            .build()

        val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(TAG_MEDICATION_REMINDER)
            .addTag(workTag)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workTag,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelMedicationAlerts(context: Context, medicationId: Long) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("med_${medicationId}")
    }

    private fun calculateInitialDelay(scheduledTime: String): Long {
        val parsedCal = parseTimeStringToTodayCalendar(scheduledTime) ?: return 60_000L
        val now = Calendar.getInstance()

        if (parsedCal.before(now)) {
            parsedCal.add(Calendar.DAY_OF_YEAR, 1) // Next day
        }
        return (parsedCal.timeInMillis - now.timeInMillis).coerceAtLeast(10_000L)
    }

    private fun parseTimeStringToTodayCalendar(timeStr: String): Calendar? {
        val patterns = arrayOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                val date = format.parse(timeStr.trim())
                if (date != null) {
                    val parsedCal = Calendar.getInstance().apply { time = date }
                    return Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE))
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }
}
