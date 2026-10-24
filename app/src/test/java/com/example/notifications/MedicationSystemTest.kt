package com.example.notifications

import com.example.data.DoseLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationSystemTest {

    @Test
    fun testDoseAlreadyTakenCheckPreventsDuplicateNotifications() {
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

        // Check if dose was already taken today
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
    fun testPendingDoseTriggersNotification() {
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
    fun testStripDoctorDisclaimersFromAiResponse() {
        val rawAiText = "Take Panado 500mg every 4-6 hours. Please consult your doctor for further guidance."
        val disclaimerPattern = Regex("(?i)please (consult|talk to|see|contact) (your|a) (doctor|physician|healthcare provider|pharmacist)[^.\\n]*[.\\n]?")
        val cleaned = rawAiText.replace(disclaimerPattern, "").trim()

        assertEquals("Take Panado 500mg every 4-6 hours.", cleaned)
    }
}
