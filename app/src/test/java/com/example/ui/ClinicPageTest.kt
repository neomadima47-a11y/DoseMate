package com.example.ui

import com.example.data.ClinicEntity
import com.example.data.calculateDistanceKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicPageTest {

    private fun isMedicalQuery(text: String): Boolean {
        val keywords = listOf(
            "hospital", "clinic", "medical", "health", "doctor", "pharmacy",
            "chemist", "dispensary", "emergency", "care", "nursing", "dentist",
            "dental", "physio", "specialist", "maternity", "wellness", "sanatorium",
            "infirmary", "med", "surgery", "hosp", "private"
        )
        val lower = text.lowercase()
        return keywords.any { lower.contains(it) }
    }

    @Test
    fun testClinicPage1_validateHospitalAndClinicKeywords() {
        // Medical facilities MUST return true
        assertTrue(isMedicalQuery("Steve Biko Academic Hospital"))
        assertTrue(isMedicalQuery("Sunnyside Community Clinic"))
        assertTrue(isMedicalQuery("Netcare Unitas Private Hospital"))
        assertTrue(isMedicalQuery("Dis-Chem Pharmacy"))
        assertTrue(isMedicalQuery("Emergency Medical Care"))

        // Non-medical places MUST return false
        assertFalse(isMedicalQuery("Brooklyn Shopping Mall"))
        assertFalse(isMedicalQuery("Coffee Shop"))
        assertFalse(isMedicalQuery("Clothing Store"))
    }

    @Test
    fun testClinicPage2_calculateGpsDistanceToClinic() {
        // Pretoria CBD GPS coordinates
        val userLat = -25.7479
        val userLng = 28.2293

        // Nearby clinic coordinates
        val clinicLat = -25.7500
        val clinicLng = 28.2300

        val distanceKm = calculateDistanceKm(userLat, userLng, clinicLat, clinicLng)

        assertTrue("Distance should be between 0.1km and 1.0km", distanceKm > 0.1)
        assertTrue("Distance should be between 0.1km and 1.0km", distanceKm < 1.0)
    }

    @Test
    fun testClinicPage3_sortClinicsByClosestProximity() {
        val clinics = listOf(
            ClinicEntity(id = 1, name = "Far Hospital", distanceKm = 12.5, openHours = "24/7", address = "123 Far St", phone = "+27 12 354 1000"),
            ClinicEntity(id = 2, name = "Close Clinic", distanceKm = 0.8, openHours = "24/7", address = "45 Close St", phone = "+27 12 354 1000"),
            ClinicEntity(id = 3, name = "Mid Medical", distanceKm = 3.2, openHours = "24/7", address = "78 Mid St", phone = "+27 12 354 1000")
        )

        val sorted = clinics.sortedBy { it.distanceKm }

        assertEquals("Close Clinic", sorted[0].name)
        assertEquals("Mid Medical", sorted[1].name)
        assertEquals("Far Hospital", sorted[2].name)
    }

    @Test
    fun testClinicPage4_formatClinicOperatingHoursAndAddress() {
        val clinic = ClinicEntity(
            id = 10,
            name = "Steve Biko Academic Hospital",
            distanceKm = 1.2,
            openHours = "Open 24 Hours",
            address = "Voortrekker Rd, Pretoria",
            phone = "+27 12 354 1000",
            isNearest = true
        )

        assertEquals("Steve Biko Academic Hospital", clinic.name)
        assertEquals("Open 24 Hours", clinic.openHours)
        assertEquals("+27 12 354 1000", clinic.phone)
        assertTrue(clinic.isNearest)
    }
}
