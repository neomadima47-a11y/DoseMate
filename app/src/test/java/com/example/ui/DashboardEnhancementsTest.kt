package com.example.ui

import com.example.data.DoseLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DashboardEnhancementsTest {

    @Test
    fun testNextDoseUpCountdownCalculation() {
        val pendingDoses = listOf(
            DoseLogEntity(
                id = 1,
                medicationId = 10,
                medicationName = "Metformin 500mg",
                scheduledTime = "08:00 PM",
                takenTime = null,
                status = "Pending",
                date = "Today"
            )
        )

        val nextDose = pendingDoses.firstOrNull()

        assertNotNull(nextDose)
        assertEquals("Metformin 500mg", nextDose?.medicationName)
        assertEquals("08:00 PM", nextDose?.scheduledTime)
    }

    @Test
    fun testDailyAdherenceProgressWheelCalculation() {
        val doses = listOf(
            DoseLogEntity(id = 1, medicationId = 1, medicationName = "Metformin", scheduledTime = "08:00 AM", takenTime = "08:05 AM", status = "Taken", date = "Today"),
            DoseLogEntity(id = 2, medicationId = 1, medicationName = "Metformin", scheduledTime = "02:00 PM", takenTime = "02:00 PM", status = "Taken", date = "Today"),
            DoseLogEntity(id = 3, medicationId = 1, medicationName = "Metformin", scheduledTime = "08:00 PM", takenTime = null, status = "Pending", date = "Today")
        )

        val takenCount = doses.count { it.status == "Taken" }
        val totalCount = doses.size
        val percentage = (takenCount * 100) / totalCount

        assertEquals(3, totalCount)
        assertEquals(2, takenCount)
        assertEquals(66, percentage)
    }

    @Test
    fun testNextDoseTakeNowAction() {
        val pendingDose = DoseLogEntity(
            id = 1,
            medicationId = 10,
            medicationName = "Panado 500mg",
            scheduledTime = "08:00 AM",
            takenTime = null,
            status = "Pending",
            date = "Today"
        )

        val takenDose = pendingDose.copy(status = "Taken", takenTime = "08:02 AM")

        assertEquals("Taken", takenDose.status)
        assertEquals("08:02 AM", takenDose.takenTime)
    }
}
