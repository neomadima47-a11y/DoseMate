package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra("medication_id", -1L)
        val medicationName = intent.getStringExtra("medication_name") ?: "Medication"
        val dosage = intent.getStringExtra("dosage") ?: ""
        val scheduledTime = intent.getStringExtra("scheduled_time") ?: ""

        if (medicationId == -1L) return

        Log.d("MedicationAlarmReceiver", "Exact alarm fired for $medicationName ($dosage) scheduled at $scheduledTime")

        // Display High-Priority Heads-Up Pop-Up Notification with Sound & Vibration
        MedicationNotificationHelper.showMedicationReminder(
            context = context,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            scheduledTime = scheduledTime
        )

        // Reschedule alarm for tomorrow at the same scheduled time
        MedicationNotificationScheduler.scheduleNextDailyOccurrence(
            context = context,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            scheduledTime = scheduledTime
        )
    }
}
