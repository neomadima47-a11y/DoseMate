package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object MedicationNotificationHelper {

    const val CHANNEL_ID = "medication_reminders_channel"
    private const val CHANNEL_NAME = "Medication Alerts"
    private const val CHANNEL_DESC = "High priority local medication reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showMedicationReminder(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        scheduledTime: String
    ) {
        createNotificationChannel(context)

        val notificationId = (medicationId * 1000 + scheduledTime.hashCode() % 1000).toInt().coerceAtLeast(1)

        // Open app intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "medications")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick "Mark Taken" intent directly from the notification tray
        val markTakenIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = MedicationActionReceiver.ACTION_MARK_TAKEN
            putExtra(MedicationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationActionReceiver.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(MedicationActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(MedicationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 5000,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Time for $medicationName ($dosage)")
            .setContentText("Scheduled for $scheduledTime. Tap to log or confirm.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_check_circle, "✓ Mark Taken", markTakenPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}
