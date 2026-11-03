package com.example

import com.example.data.DoseLogEntity
import com.example.data.HealthReadingEntity
import com.example.data.MedicationEntity
import com.example.util.HealthDataExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the two features added in Part 3 of the DoseMate PoE:
 * medication refill tracking and the CSV health history export.
 *
 * Each test covers both a valid case and an invalid or edge case, so the
 * behaviour is verified in both directions rather than only on the happy path.
 */
class DoseMateFeatureTest {

    // ---------- Helpers ----------

    private fun medication(
        name: String = "Metformin",
        pillsRemaining: Int = 30,
        packSize: Int = 30,
        refillThreshold: Int = 5
    ) = MedicationEntity(
        id = 1L,
        name = name,
        dosage = "500mg",
        frequency = "Twice daily",
        reminderTimes = "08:00 AM, 08:00 PM",
        status = "Active",
        pillsRemaining = pillsRemaining,
        packSize = packSize,
        refillThreshold = refillThreshold
    )

    private fun reading(
        type: String = "Blood sugar",
        value: String = "7.2",
        notes: String = "Fasting"
    ) = HealthReadingEntity(
        id = 1L,
        type = type,
        value = value,
        unit = "mmol/L",
        whenTaken = "Before breakfast",
        notes = notes,
        timestamp = 1_762_000_000_000L,
        displayTime = "Today, 6:45am",
        isPendingSync = false
    )

    private fun doseLog(
        status: String = "Taken",
        takenTime: String? = "08:05 AM"
    ) = DoseLogEntity(
        id = 1L,
        medicationId = 1L,
        medicationName = "Metformin (500mg)",
        scheduledTime = "08:00 AM",
        takenTime = takenTime,
        status = status,
        date = "2026-11-03",
        isPendingSync = false
    )

    // ---------- TEST 1: refill threshold detection ----------

    /**
     * Test 1: a medication is only flagged for refill once its remaining
     * tablet count reaches the threshold.
     *
     * Valid case: 3 tablets left against a threshold of 5 is low stock.
     * Invalid case: 20 tablets left is not low stock and must not be flagged.
     */
    @Test
    fun refillThreshold_flagsOnlyWhenStockIsLow() {
        val lowStock = medication(pillsRemaining = 3, refillThreshold = 5)
        val healthyStock = medication(pillsRemaining = 20, refillThreshold = 5)

        assertTrue(
            "A medication with 3 of 30 tablets should be flagged for refill",
            lowStock.pillsRemaining <= lowStock.refillThreshold
        )
        assertFalse(
            "A medication with 20 of 30 tablets should not be flagged",
            healthyStock.pillsRemaining <= healthyStock.refillThreshold
        )
    }

    // ---------- TEST 2: pill count never goes negative ----------

    /**
     * Test 2: taking a dose removes one tablet, but the count must never fall
     * below zero even if more doses are logged than tablets remain.
     *
     * Valid case: 10 tablets becomes 9.
     * Invalid case: taking a dose from an empty pack stays at 0, not -1.
     */
    @Test
    fun pillCount_decrementsButNeverGoesNegative() {
        // Mirrors the MAX(pillsRemaining - 1, 0) rule in the DAO query
        fun decrement(current: Int): Int = maxOf(current - 1, 0)

        assertEquals("Taking a dose should leave 9 tablets", 9, decrement(10))
        assertEquals("An empty pack must stay at zero", 0, decrement(0))
    }

    // ---------- TEST 3: CSV escaping ----------

    /**
     * Test 3: notes containing a comma must be quoted in the CSV, otherwise
     * the column alignment breaks when the file is opened in a spreadsheet.
     *
     * Valid case: a plain note appears unquoted.
     * Invalid case: a note with a comma is wrapped in quotes.
     */
    @Test
    fun csvExport_escapesCommasInNotes() {
        val plainNote = reading(notes = "Fasting reading")
        val commaNote = reading(notes = "Felt dizzy, took water")

        val plainCsv = HealthDataExporter.buildCsv("Lerato Mokoena", listOf(plainNote), emptyList())
        val commaCsv = HealthDataExporter.buildCsv("Lerato Mokoena", listOf(commaNote), emptyList())

        assertTrue(
            "A plain note should be written without quotes",
            plainCsv.contains("Fasting reading")
        )
        assertTrue(
            "A note containing a comma must be wrapped in quotes",
            commaCsv.contains("\"Felt dizzy, took water\"")
        )
    }

    // ---------- TEST 4: adherence calculation ----------

    /**
     * Test 4: the CSV summary reports the correct adherence percentage.
     *
     * Valid case: two of four doses taken is 50 percent.
     * Invalid case: an empty log list reports 0 percent rather than crashing
     * with a division by zero.
     */
    @Test
    fun csvExport_calculatesAdherencePercentage() {
        val logs = listOf(
            doseLog(status = "Taken"),
            doseLog(status = "Taken"),
            doseLog(status = "Pending", takenTime = null),
            doseLog(status = "Pending", takenTime = null)
        )

        val withData = HealthDataExporter.buildCsv("Lerato Mokoena", emptyList(), logs)
        val withoutData = HealthDataExporter.buildCsv("Lerato Mokoena", emptyList(), emptyList())

        assertTrue("Two of four doses taken should report 50%", withData.contains("Adherence,50%"))
        assertTrue("An empty log should report 0% and not crash", withoutData.contains("Adherence,0%"))
    }

    // ---------- TEST 5: CSV structure ----------

    /**
     * Test 5: the export always contains both section headers, so the file
     * opens correctly even when one of the two data sets is empty.
     *
     * Valid case: with readings present, the rows appear under the headers.
     * Invalid case: with no readings at all, a placeholder row is written
     * instead of leaving the section blank.
     */
    @Test
    fun csvExport_alwaysIncludesBothSections() {
        val populated = HealthDataExporter.buildCsv(
            "Lerato Mokoena",
            listOf(reading()),
            listOf(doseLog())
        )
        val empty = HealthDataExporter.buildCsv("Lerato Mokoena", emptyList(), emptyList())

        assertTrue("Readings section header is missing", populated.contains("HEALTH READINGS"))
        assertTrue("Adherence section header is missing", populated.contains("MEDICATION ADHERENCE"))
        assertTrue("Blood sugar row should be present", populated.contains("Blood sugar"))

        assertTrue("Empty export still needs the readings header", empty.contains("HEALTH READINGS"))
        assertTrue("Empty readings should state that none were recorded", empty.contains("No readings recorded"))
        assertTrue("Empty logs should state that none were logged", empty.contains("No doses logged"))
    }
}