package com.example.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.DoseLogEntity
import com.example.data.HealthBridgeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_TAKEN = "com.example.notifications.ACTION_MARK_TAKEN"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MARK_TAKEN) {
            val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
            val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medication"
            val scheduledTime = intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: ""
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

            if (notificationId != -1) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(notificationId)
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HealthBridgeDatabase.getDatabase(context)
                    val medicationDao = db.medicationDao()

                    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val currentTimeStr = timeFormat.format(Date())
                    val currentDateStr = dateFormat.format(Date())

                    val logs = medicationDao.getAllDoseLogs().first()
                    val matchingLog = logs.firstOrNull { log ->
                        log.medicationId == medicationId &&
                            (log.date == "Today" || log.date == currentDateStr) &&
                            log.status != "Taken"
                    }

                    if (matchingLog != null) {
                        medicationDao.updateDoseStatus(matchingLog.id, "Taken", currentTimeStr)
                    } else {
                        medicationDao.insertDoseLogs(
                            listOf(
                                DoseLogEntity(
                                    medicationId = medicationId,
                                    medicationName = medicationName,
                                    scheduledTime = scheduledTime,
                                    takenTime = currentTimeStr,
                                    status = "Taken",
                                    date = currentDateStr
                                )
                            )
                        )
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "$medicationName marked as taken!", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
