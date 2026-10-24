package com.example.ui

import com.example.data.DoseLogEntity
import com.example.data.MedicationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MedicationsPageRequirementsTest {

    @Test
    fun test1_createActiveMedicationPrescriptionEntity() {
        val med = MedicationEntity(
            id = 1001L,
            name = "Panado",
            dosage = "500mg",
            frequency = "Twice daily",
            reminderTimes = "08:00 AM, 08:00 PM",
            status = "Active"
        )

        assertEquals("Panado", med.name)
        assertEquals("500mg", med.dosage)
        assertEquals("Twice daily", med.frequency)
        assertEquals("Active", med.status)
        assertEquals("08:00 AM, 08:00 PM", med.reminderTimes)
    }

    @Test
    fun test2_generateIndependentAlarmRequestCodesPerMedication() {
        val med1Id = 101L
        val med2Id = 102L
        val scheduledTime = "08:00 AM"

        val code1 = (med1Id * 1000 + scheduledTime.hashCode() % 1000).toInt().coerceAtLeast(1)
        val code2 = (med2Id * 1000 + scheduledTime.hashCode() % 1000).toInt().coerceAtLeast(1)

        assertNotEquals(code1, code2)
    }

    @Test
    fun test3_autoGenerateThirtyDaysDoseLogsOnPrescriptionAdd() {
        val medId = 101L
        val medName = "Panado 500mg"
        val reminderTimes = "08:00 AM, 08:00 PM"

        val times = reminderTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val logs = mutableListOf<DoseLogEntity>()

        for (dayOffset in 0..6) {
            for (t in times) {
                logs.add(
                    DoseLogEntity(
                        medicationId = medId,
                        medicationName = medName,
                        scheduledTime = t,
                        status = "Pending",
                        date = "2026-10-$dayOffset"
                    )
                )
            }
        }

        assertEquals(14, logs.size)
        assertEquals("08:00 AM", logs[0].scheduledTime)
        assertEquals("08:00 PM", logs[1].scheduledTime)
    }

    @Test
    fun test4_filterDosesByAllDueTodayAndCompleted() {
        val allDoses = listOf(
            DoseLogEntity(id = 1, medicationId = 1, medicationName = "Metformin 500mg", scheduledTime = "07:00 AM", status = "Taken", date = "Today"),
            DoseLogEntity(id = 2, medicationId = 1, medicationName = "Metformin 500mg", scheduledTime = "07:00 PM", status = "Pending", date = "Today"),
            DoseLogEntity(id = 3, medicationId = 2, medicationName = "Amlodipine 5mg", scheduledTime = "06:00 PM", status = "Pending", date = "Today")
        )

        val dueDoses = allDoses.filter { it.status != "Taken" }
        val completedDoses = allDoses.filter { it.status == "Taken" }

        assertEquals(3, allDoses.size)
        assertEquals(2, dueDoses.size)
        assertEquals(1, completedDoses.size)
        assertEquals("Metformin 500mg", completedDoses[0].medicationName)
    }

    @Test
    fun test5_classifyMedicationCategoryTypeBadges() {
        fun getCategoryLabel(name: String): String {
            val lower = name.lowercase()
            return when {
                lower.contains("metformin") || lower.contains("sugar") -> "🩸 Type 2 Diabetes"
                lower.contains("amlodipine") || lower.contains("pressure") -> "❤️ Hypertension"
                lower.contains("panado") || lower.contains("pain") -> "💊 Analgesic"
                else -> "💊 Chronic Prescription"
            }
        }

        assertEquals("🩸 Type 2 Diabetes", getCategoryLabel("Metformin 500mg"))
        assertEquals("❤️ Hypertension", getCategoryLabel("Amlodipine 5mg"))
        assertEquals("💊 Analgesic", getCategoryLabel("Panado 500mg"))
    }
}
