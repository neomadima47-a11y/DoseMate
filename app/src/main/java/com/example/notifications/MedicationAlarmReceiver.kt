package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.HealthBridgeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra("medication_id", -1L)
        val medicationName = intent.getStringExtra("medication_name") ?: "Medication"
        val dosage = intent.getStringExtra("dosage") ?: ""
        val scheduledTime = intent.getStringExtra("scheduled_time") ?: ""

        if (medicationId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HealthBridgeDatabase.getDatabase(context)
                val dao = db.medicationDao()

                // 1. Verify the medication exists and is still active
                val allMeds = dao.getAllMedications().first()
                val activeMed = allMeds.find { it.id == medicationId && it.status.equals("Active", ignoreCase = true) }

                if (activeMed != null) {
                    // 2. Check if the dose for today has ALREADY been taken
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val todayStr = dateFormat.format(Date())
                    val logs = dao.getAllDoseLogs().first()

                    val isAlreadyTakenToday = logs.any { log ->
                        log.medicationId == medicationId &&
                                (log.date == "Today" || log.date == todayStr) &&
                                log.status.equals("Taken", ignoreCase = true)
                    }

                    // 3. ONLY trigger the notification popup if the dose was NOT taken yet today
                    if (!isAlreadyTakenToday) {
                        MedicationNotificationHelper.showMedicationReminder(
                            context = context,
                            medicationId = medicationId,
                            medicationName = medicationName,
                            dosage = dosage,
                            scheduledTime = scheduledTime
                        )
                    } else {
                        Log.d("MedicationAlarmReceiver", "Dose already taken today for $medicationName ($dosage). Skipping notification.")
                    }

                    // 4. Schedule next daily occurrence for tomorrow at the same time
                    MedicationNotificationScheduler.scheduleNextDailyOccurrence(
                        context = context,
                        medicationId = medicationId,
                        medicationName = medicationName,
                        dosage = dosage,
                        scheduledTime = scheduledTime
                    )
                } else {
                    Log.d("MedicationAlarmReceiver", "Medication ID $medicationId is no longer active. Skipping alarm notification.")
                }
            } catch (e: Exception) {
                Log.e("MedicationAlarmReceiver", "Error checking alarm notification status", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
