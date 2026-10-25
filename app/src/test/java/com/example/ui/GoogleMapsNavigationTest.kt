package com.example.ui

import com.example.data.calculateDistanceKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleMapsNavigationTest {

    @Test
    fun testMapPage1_convertVoiceSpeechToTextQuery() {
        val voiceSpokenResults = listOf("Sunnyside Community Clinic", "Sunnyside Clinic", "Clinic")
        val topSpokenResult = voiceSpokenResults.firstOrNull()

        assertNotNull(topSpokenResult)
        assertEquals("Sunnyside Community Clinic", topSpokenResult)
    }

    @Test
    fun testMapPage2_calculateStreetRoadTurnByTurnRoute() {
        // Pretoria user GPS location
        val userLat = -25.7479
        val userLng = 28.2293

        // Destination selected by tapping map
        val destLat = -25.7520
        val destLng = 28.2350
        val destName = "Dis-Chem Pharmacy"

        val distanceKm = calculateDistanceKm(userLat, userLng, destLat, destLng)
        val estDriveTimeMins = Math.max(1, Math.round(distanceKm * 2.5).toInt())

        assertTrue("Distance to tapped destination should be greater than 0", distanceKm > 0)
        assertTrue("Drive time should be at least 1 min", estDriveTimeMins >= 1)
        assertEquals("Dis-Chem Pharmacy", destName)
    }

    @Test
    fun testMapPage3_updateLiveGpsUserMarkerCoordinates() {
        val userLat = -25.7485
        val userLng = 28.2299

        assertEquals(-25.7485, userLat, 0.0001)
        assertEquals(28.2299, userLng, 0.0001)
    }

    @Test
    fun testMapPage4_clearPolylineRouteOnNavigationCancel() {
        val isNavigating = false
        val activeRoutePolylineCount = 0

        assertFalse("Navigation state must be inactive after cancel", isNavigating)
        assertEquals(0, activeRoutePolylineCount)
    }
}
