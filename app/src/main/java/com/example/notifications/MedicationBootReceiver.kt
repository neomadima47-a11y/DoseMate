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

class MedicationBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("MedicationBootReceiver", "Device boot detected. Rescheduling all medication alarms...")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HealthBridgeDatabase.getDatabase(context)
                    val activeMeds = db.medicationDao().getAllMedications().first().filter { it.status == "Active" }
                    MedicationNotificationScheduler.scheduleAllActiveMedications(context, activeMeds)
                } catch (e: Exception) {
                    Log.e("MedicationBootReceiver", "Failed to reschedule alarms on boot", e)
                }
            }
        }
    }
}
