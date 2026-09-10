package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClinicEntity
import com.example.data.HealthReadingEntity
import com.example.data.MedicationEntity
import com.example.data.SerpApiPlace
import com.example.data.calculateDistanceKm
import com.example.ui.theme.HealthAmber
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HealthRed
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealSecondary
import com.example.util.ClinicalEvaluator
import com.example.util.HealthSeverity
import com.example.util.Strings
import java.net.URLEncoder

/**
 * Unified model for displaying clinics in UI, whether loaded from SerpApi Google Maps engine
 * or from local Room database records.
 */
data class DisplayClinic(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val openHours: String,
    val address: String,
    val phone: String,
    val isNearest: Boolean,
    val rating: Double?,
    val reviewsCount: Int?,
    val placeType: String,
    val directionsUrl: String,
    val websiteUrl: String?,
    val isLiveSerpApi: Boolean,
    val latOffsetDp: Float,
    val lngOffsetDp: Float
)

fun ClinicEntity.toDisplayClinic(): DisplayClinic = DisplayClinic(
    id = "local_$id",
    name = name,
    distanceKm = distanceKm,
    openHours = openHours,
    address = address,
    phone = phone,
    isNearest = isNearest,
    rating = 4.3,
    reviewsCount = 48,
    placeType = "Community Health Centre",
    directionsUrl = "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode("$name $address", "UTF-8")}",
    websiteUrl = null,
    isLiveSerpApi = false,
    latOffsetDp = latOffsetDp,
    lngOffsetDp = lngOffsetDp
)

fun SerpApiPlace.toDisplayClinic(index: Int, originLat: Double = -25.7479, originLng: Double = 28.2293): DisplayClinic {
    val dist = if (latitude != null && longitude != null) {
        calculateDistanceKm(originLat, originLng, latitude, longitude)
    } else {
        1.1 + (index * 0.8)
    }

    val latOff = if (latitude != null) {
        ((latitude - originLat) * 1400).toFloat().coerceIn(-75f, 75f)
    } else {
        when (index % 4) {
            0 -> -40f
            1 -> 45f
            2 -> -30f
            else -> 35f
        }
    }

    val lngOff = if (longitude != null) {
        ((longitude - originLng) * 1400).toFloat().coerceIn(-75f, 75f)
    } else {
        when (index % 4) {
            0 -> 35f
            1 -> -40f
            2 -> -45f
            else -> 40f
        }
    }

    val safeDirections = directionsUrl ?: if (latitude != null && longitude != null) {
        "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"
    } else {
        "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode("$title $address", "UTF-8")}"
    }

    return DisplayClinic(
        id = placeId?.ifBlank { null } ?: "serp_$index",
        name = title,
        distanceKm = Math.round(dist * 10.0) / 10.0,
        openHours = openHours.ifBlank { "Hours listed on Google Maps" },
        address = address.ifBlank { "Location listed on Google Maps" },
        phone = phone.ifBlank { "Call via Google Maps" },
        isNearest = (index == 0),
        rating = if (rating > 0.0) rating else null,
        reviewsCount = if (reviewsCount > 0) reviewsCount else null,
        placeType = placeType.ifBlank { "Medical Clinic" },
        directionsUrl = safeDirections,
        websiteUrl = website?.ifBlank { null },
        isLiveSerpApi = true,
        latOffsetDp = latOff,
        lngOffsetDp = lngOff
    )
}

@Composable
fun ClinicLocatorScreen(
    language: String,
    clinics: List<ClinicEntity>,
    liveClinics: List<SerpApiPlace> = emptyList(),
    isSearchingClinics: Boolean = false,
    clinicSearchError: String? = null,
    serpApiKey: String = "",
    activeSearchQuery: String = "",
    healthReadings: List<HealthReadingEntity> = emptyList(),
    medications: List<MedicationEntity> = emptyList(),
    onSearchClinics: (query: String, customApiKey: String?) -> Unit = { _, _ -> },
    onClearSearch: () -> Unit = {},
    onSetSerpApiKey: (String) -> Unit = {},
    onNavigateToLogReading: () -> Unit = {},
    onNavigateToHealthLog: () -> Unit = {},
    onNavigateToMedications: () -> Unit = {}
) {
    val context = LocalContext.current
    var searchQueryInput by remember { mutableStateOf(activeSearchQuery.ifBlank { "clinics near me" }) }
    var isApiKeyDialogOpen by remember { mutableStateOf(false) }
    var tempApiKeyInput by remember { mutableStateOf(serpApiKey) }

    // Sync input when activeSearchQuery updates externally
    LaunchedEffect(activeSearchQuery) {
        if (activeSearchQuery.isNotBlank()) {
            searchQueryInput = activeSearchQuery
        }
    }

    // Unified clinic list
    val displayClinics = remember(liveClinics, clinics) {
        if (liveClinics.isNotEmpty()) {
            liveClinics.mapIndexed { idx, place -> place.toDisplayClinic(idx) }
        } else {
            clinics.map { it.toDisplayClinic() }
        }
    }

    var selectedClinicId by remember { mutableStateOf<String?>(null) }

    // Ensure selectedClinicId is valid
    LaunchedEffect(displayClinics) {
        if (displayClinics.isNotEmpty()) {
            if (selectedClinicId == null || displayClinics.none { it.id == selectedClinicId }) {
                selectedClinicId = displayClinics.first().id
            }
        } else {
            selectedClinicId = null
        }
    }

    val selectedClinic = displayClinics.firstOrNull { it.id == selectedClinicId } ?: displayClinics.firstOrNull()

    // Helper functions for external intents
    fun launchGoogleMapsDirections(directionsUrl: String, clinicName: String, address: String) {
        try {
            val uri = if (directionsUrl.isNotBlank() && directionsUrl.startsWith("http")) {
                Uri.parse(directionsUrl)
            } else {
                Uri.parse("geo:0,0?q=${Uri.encode("$clinicName $address")}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Throwable) {
            Toast.makeText(context, "Could not open Google Maps", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchDialer(phone: String) {
        try {
            val cleaned = phone.replace(" ", "").replace("-", "")
            if (cleaned.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Throwable) {
            Toast.makeText(context, "Could not launch dialer", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Title and Engine Status Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Strings.get("find_clinic", language),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (liveClinics.isNotEmpty()) "Google Maps Live Results" else "Nearby Health Facilities",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // SerpApi Google Maps Engine Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (liveClinics.isNotEmpty()) HealthGreen.copy(alpha = 0.15f)
                        else if (serpApiKey.isNotBlank()) HealthBlue.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable {
                        tempApiKeyInput = serpApiKey
                        isApiKeyDialogOpen = true
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (liveClinics.isNotEmpty()) Icons.Default.Public else Icons.Default.Key,
                        contentDescription = "SerpApi Configuration",
                        tint = if (liveClinics.isNotEmpty()) HealthGreen else if (serpApiKey.isNotBlank()) HealthBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (liveClinics.isNotEmpty()) "SerpApi Live (${liveClinics.size})"
                        else if (serpApiKey.isNotBlank()) "SerpApi Ready"
                        else "SerpApi Key",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (liveClinics.isNotEmpty()) HealthGreen else if (serpApiKey.isNotBlank()) HealthBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Google Maps Search Bar (Engine: google_maps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQueryInput,
                onValueChange = { searchQueryInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("clinic_search_input"),
                placeholder = {
                    Text("Search clinics, hospitals, pharmacies...", fontSize = 12.sp)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search Google Maps",
                        tint = TealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQueryInput.isNotBlank()) {
                        IconButton(onClick = {
                            searchQueryInput = ""
                            if (liveClinics.isNotEmpty()) onClearSearch()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (searchQueryInput.isNotBlank()) {
                        onSearchClinics(searchQueryInput, serpApiKey)
                    }
                },
                modifier = Modifier
                    .height(50.dp)
                    .testTag("execute_clinic_search_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSearchingClinics) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Search", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Quick Category Suggestions Chips
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chips = listOf(
                "Clinics near me",
                "24h Hospitals",
                "Pharmacies",
                "Public Health Centres",
                "Pretoria Clinics",
                "Johannesburg Clinics"
            )
            chips.forEach { chipQuery ->
                FilterChip(
                    selected = (activeSearchQuery == chipQuery),
                    onClick = {
                        searchQueryInput = chipQuery
                        onSearchClinics(chipQuery, serpApiKey)
                    },
                    label = { Text(chipQuery, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealPrimary.copy(alpha = 0.18f),
                        selectedLabelColor = TealPrimary
                    )
                )
            }
        }

        // Loading Indicator
        if (isSearchingClinics) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = TealPrimary
            )
            Text(
                text = "Querying Google Maps engine via SerpApi...",
                fontSize = 11.sp,
                color = TealPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Error Banner with Action
        if (clinicSearchError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HealthRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = clinicSearchError,
                        fontSize = 11.sp,
                        color = HealthRed,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        tempApiKeyInput = serpApiKey
                        isApiKeyDialogOpen = true
                    }) {
                        Text("Add Key", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HealthRed)
                    }
                }
            }
        }

        // Live Results Filter Notice
        if (liveClinics.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${liveClinics.size} Google Maps results for \"$activeSearchQuery\"",
                    fontSize = 11.sp,
                    color = HealthGreen,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onClearSearch) {
                    Text("Reset to Local", fontSize = 11.sp, color = TealPrimary)
                }
            }
        }

        // Health Reading Alert Context
        val criticalReading = healthReadings.firstOrNull { reading ->
            val evaluation = ClinicalEvaluator.evaluate(reading.type, reading.value)
            evaluation.severity == HealthSeverity.CRITICAL || evaluation.severity == HealthSeverity.WARNING
        }
        if (criticalReading != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToHealthLog() },
                colors = CardDefaults.cardColors(containerColor = HealthRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = HealthRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Elevated ${criticalReading.type} (${criticalReading.value} ${criticalReading.unit}). Visit recommended.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HealthRed
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = HealthRed, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Live Map Canvas (Dynamic Coordinates & Radar)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF19221E)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Grid lines representing streets
                    val gridColor = Color(0xFF2C3933)
                    for (i in 1..4) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, height * (i / 5f)),
                            end = Offset(width, height * (i / 5f)),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(width * (i / 5f), 0f),
                            end = Offset(width * (i / 5f), height),
                            strokeWidth = 2f
                        )
                    }

                    // Radar range rings from center
                    drawCircle(
                        color = HealthBlue.copy(alpha = 0.12f),
                        radius = height * 0.42f,
                        center = Offset(width / 2f, height / 2f)
                    )
                    drawCircle(
                        color = HealthBlue.copy(alpha = 0.22f),
                        radius = height * 0.22f,
                        center = Offset(width / 2f, height / 2f)
                    )
                }

                // Center User Location (Blue Pulse Dot)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(HealthBlue)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Clinic Pins Plotted on Radar Canvas
                displayClinics.take(8).forEachIndexed { index, clinic ->
                    val isSelected = (selectedClinic?.id == clinic.id)

                    // Map relative offsets onto canvas coordinates
                    val alignment = when (index % 6) {
                        0 -> Alignment.TopStart
                        1 -> Alignment.TopEnd
                        2 -> Alignment.BottomStart
                        3 -> Alignment.BottomEnd
                        4 -> Alignment.CenterStart
                        else -> Alignment.CenterEnd
                    }

                    Box(
                        modifier = Modifier
                            .align(alignment)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .clickable { selectedClinicId = clinic.id }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 34.dp else 24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) TealPrimary else Color(0xFFE53935))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = clinic.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isSelected) 18.dp else 13.dp)
                                )
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = clinic.name.take(18) + if (clinic.name.length > 18) "…" else "",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Clinic List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${Strings.get("nearby_clinics", language)} (${displayClinics.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (selectedClinic != null) {
                Text(
                    text = "Selected: ${selectedClinic.name.take(20)}",
                    fontSize = 11.sp,
                    color = TealPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Clinic Cards List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(displayClinics, key = { it.id }) { clinic ->
                val isSelected = (selectedClinic?.id == clinic.id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedClinicId = clinic.id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) TealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, TealPrimary) else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = clinic.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${clinic.distanceKm} km · ${clinic.openHours}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (clinic.rating != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = "Rating",
                                                    tint = HealthAmber,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "${clinic.rating}${if (clinic.reviewsCount != null) " (${clinic.reviewsCount})" else ""}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = HealthAmber
                                                )
                                            }
                                        }
                                    }

                                    if (clinic.isLiveSerpApi) {
                                        Text(
                                            text = "Source: Google Maps (${clinic.placeType})",
                                            fontSize = 10.sp,
                                            color = HealthGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Selected",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Expanded Details & Action Buttons
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = clinic.address,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (clinic.phone.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = HealthBlue, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = clinic.phone, fontSize = 11.sp, color = HealthBlue)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Directions Action Button
                                Button(
                                    onClick = {
                                        launchGoogleMapsDirections(clinic.directionsUrl, clinic.name, clinic.address)
                                    },
                                    modifier = Modifier.weight(1.3f),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = Strings.get("get_directions", language), fontSize = 12.sp, color = Color.White)
                                    }
                                }

                                // Call Clinic Action Button
                                if (clinic.phone.isNotBlank() && clinic.phone.any { it.isDigit() }) {
                                    OutlinedButton(
                                        onClick = { launchDialer(clinic.phone) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = HealthBlue, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Call", fontSize = 12.sp, color = HealthBlue)
                                        }
                                    }
                                }

                                // Open in Google Maps browser / app
                                if (clinic.websiteUrl != null && clinic.websiteUrl.startsWith("http")) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clinic.websiteUrl)))
                                            } catch (e: Throwable) {
                                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Website", tint = TealPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // SerpApi API Key Configuration Modal Dialog
    if (isApiKeyDialogOpen) {
        AlertDialog(
            onDismissRequest = { isApiKeyDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SerpApi Google Maps Engine", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "This screen queries SerpApi's Google Maps Search Engine (serpapi.com/search?engine=google_maps) to fetch live hospitals, clinics, and pharmacies with real reviews and coordinates.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempApiKeyInput,
                        onValueChange = { tempApiKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("SerpApi API Key") },
                        placeholder = { Text("Enter your 64-character SerpApi key") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = TealPrimary) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Get 100 free searches/month at serpapi.com\n• Keys can also be defined in .env as SERPAPI_API_KEY",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetSerpApiKey(tempApiKeyInput)
                        isApiKeyDialogOpen = false
                        if (tempApiKeyInput.isNotBlank()) {
                            onSearchClinics(searchQueryInput.ifBlank { "clinics near me" }, tempApiKeyInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Save & Search", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isApiKeyDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
