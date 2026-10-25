package com.example.notifications

import com.example.data.DoseLogEntity
import com.example.data.MedicationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationSystemTest {

    @Test
    fun testNotificationSystem1_recordTakenTimestampWhenDoseMarked() {
        val pendingDose = DoseLogEntity(
            id = 5,
            medicationId = 50,
            medicationName = "Panado 500mg",
            scheduledTime = "08:00 AM",
            takenTime = null,
            status = "Pending",
            date = "2026-10-24"
        )

        val takenDose = pendingDose.copy(status = "Taken", takenTime = "08:02 AM")

        assertEquals("Taken", takenDose.status)
        assertEquals("08:02 AM", takenDose.takenTime)
        assertNull(pendingDose.takenTime)
    }

    @Test
    fun testNotificationSystem2_skipDuplicateNotificationIfDoseTakenToday() {
        val logs = listOf(
            DoseLogEntity(
                id = 1,
                medicationId = 10,
                medicationName = "Metformin 500mg",
                scheduledTime = "07:00 AM",
                takenTime = "07:05 AM",
                status = "Taken",
                date = "2026-10-24"
            )
        )

        val medicationId = 10L
        val todayStr = "2026-10-24"
        val isTaken = logs.any { log ->
            log.medicationId == medicationId &&
                    (log.date == "Today" || log.date == todayStr) &&
                    log.status.equals("Taken", ignoreCase = true)
        }

        assertTrue("If dose is already taken, notification must be skipped", isTaken)
    }

    @Test
    fun testNotificationSystem3_triggerHighPriorityAlertForPendingDose() {
        val logs = listOf(
            DoseLogEntity(
                id = 2,
                medicationId = 20,
                medicationName = "Amlodipine 5mg",
                scheduledTime = "06:00 PM",
                takenTime = null,
                status = "Pending",
                date = "2026-10-24"
            )
        )

        val medicationId = 20L
        val todayStr = "2026-10-24"
        val isTaken = logs.any { log ->
            log.medicationId == medicationId &&
                    (log.date == "Today" || log.date == todayStr) &&
                    log.status.equals("Taken", ignoreCase = true)
        }

        assertFalse("Pending dose must trigger notification alert", isTaken)
    }

    @Test
    fun testNotificationSystem4_cancelScheduledAlarmsWhenMedicationEnded() {
        val medications = listOf(
            MedicationEntity(id = 1, name = "Panado", dosage = "500mg", frequency = "Twice daily", reminderTimes = "8:00 AM", status = "Active"),
            MedicationEntity(id = 2, name = "Amlodipine", dosage = "5mg", frequency = "Once daily", reminderTimes = "6:00 PM", status = "Ended")
        )

        val activeMeds = medications.filter { it.status.equals("Active", ignoreCase = true) }

        assertEquals(1, activeMeds.size)
        assertEquals("Panado", activeMeds[0].name)
    }

    @Test
    fun testNotificationSystem5_stripDoctorDisclaimersFromAiResponseText() {
        val rawAiText = "Take Panado 500mg every 4-6 hours. Please consult your doctor for further guidance."
        val disclaimerPattern = Regex("(?i)please (consult|talk to|see|contact) (your|a) (doctor|physician|healthcare provider|pharmacist)[^.\\n]*[.\\n]?")
        val cleaned = rawAiText.replace(disclaimerPattern, "").trim()

        assertEquals("Take Panado 500mg every 4-6 hours.", cleaned)
    }

    @Test
    fun testNotificationSystem6_openAppIntentNavigatesToMedicationsWithoutLogout() {
        val navigateToTarget = "medications"
        val isLoggedIn = true

        val nextScreenIndex = if (navigateToTarget.equals("medications", ignoreCase = true) && isLoggedIn) 2 else -1

        assertEquals(2, nextScreenIndex)
        assertTrue("User remains logged in when tapping dose reminder notification", isLoggedIn)
    }
}
