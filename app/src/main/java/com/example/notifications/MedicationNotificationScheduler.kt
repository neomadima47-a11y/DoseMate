package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.MedicationEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object MedicationNotificationScheduler {

    const val TAG_MEDICATION_REMINDER = "medication_reminder"

    fun scheduleAllActiveMedications(context: Context, medications: List<MedicationEntity>) {
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
            scheduleSingleDoseAlarm(
                context = context,
                medicationId = medication.id,
                medicationName = medication.name,
                dosage = medication.dosage,
                scheduledTime = timeStr
            )
        }
    }

    private fun scheduleSingleDoseAlarm(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        scheduledTime: String
    ) {
        val triggerCal = parseTimeStringToTodayCalendar(scheduledTime) ?: return
        val now = Calendar.getInstance()

        if (triggerCal.before(now)) {
            triggerCal.add(Calendar.DAY_OF_YEAR, 1) // Next day if time already passed today
        }

        val triggerTimeMs = triggerCal.timeInMillis
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra("medication_id", medicationId)
            putExtra("medication_name", medicationName)
            putExtra("dosage", dosage)
            putExtra("scheduled_time", scheduledTime)
        }

        val requestCode = (medicationId * 1000 + scheduledTime.hashCode() % 1000).toInt().coerceAtLeast(1)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMs, pendingIntent),
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
            Log.d("MedicationScheduler", "Scheduled exact alarm for $medicationName ($dosage) at ${triggerCal.time}")
        } catch (e: Exception) {
            Log.e("MedicationScheduler", "Error scheduling exact alarm", e)
        }
    }

    fun scheduleNextDailyOccurrence(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        scheduledTime: String
    ) {
        scheduleSingleDoseAlarm(
            context = context,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            scheduledTime = scheduledTime
        )
    }

    fun cancelMedicationAlerts(context: Context, medicationId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val requestCode = (medicationId * 1000).toInt().coerceAtLeast(1)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
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
