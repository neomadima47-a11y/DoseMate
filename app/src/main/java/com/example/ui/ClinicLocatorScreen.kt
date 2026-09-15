package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.ClinicEntity
import com.example.data.HealthReadingEntity
import com.example.data.MapboxRoute
import com.example.data.calculateDistanceKm
import com.example.ui.theme.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.ComposeMapInitOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.style.GenericStyle
import java.util.Locale

data class DisplayClinic(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val openHours: String,
    val address: String,
    val phone: String,
    val isNearest: Boolean,
    val hasPharmacy: Boolean = true,
    val offersMaternity: Boolean = false,
    val offersTbServices: Boolean = true,
    val offersArtClinic: Boolean = true,
    val rating: Double? = null,
    val totalReviews: Int? = null,
    val isOpenNow: Boolean? = null,
    val latitude: Double = -25.7479,
    val longitude: Double = 28.1881
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicLocatorScreen(
    clinics: List<ClinicEntity>,
    activeRoute: MapboxRoute? = null,
    selectedDestination: DisplayClinic? = null,
    onGetDirections: (DisplayClinic, String, Double, Double) -> Unit = { _, _, _, _ -> },
    onGetCustomDirections: (Double, Double, String, String, String, Double, Double) -> Unit = { _, _, _, _, _, _, _ -> },
    onClearRoute: () -> Unit = {},
    onClearDestination: () -> Unit = {},
    onSaveMapboxToken: (String) -> Unit = {},
    mapboxToken: String = "",
    recentReadings: List<HealthReadingEntity> = emptyList(),
    onNavigateToHealthLog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Location state (Defaults to Pretoria CBD)
    var userLat by remember { mutableDoubleStateOf(-25.7479) }
    var userLng by remember { mutableDoubleStateOf(28.1881) }
    var hasGpsLocation by remember { mutableStateOf(false) }
    var locationAddress by remember { mutableStateOf("Pretoria Central, Gauteng") }

    // UI state
    var selectedClinicId by remember { mutableStateOf<String?>(null) }
    var routeProfile by remember { mutableStateOf("driving-traffic") }
    var isMapExpanded by remember { mutableStateOf(false) }
    var isSimulatingRoute by remember { mutableStateOf(false) }
    var simulationProgress by remember { mutableFloatStateOf(0f) }
    var searchQuery by remember { mutableStateOf("") }
    var mapLoadError by remember { mutableStateOf<String?>(null) }

    // Request GPS permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceLocation(fusedLocationClient, context) { lat, lng, addr ->
                userLat = lat
                userLng = lng
                hasGpsLocation = true
                locationAddress = addr
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            fetchDeviceLocation(fusedLocationClient, context) { lat, lng, addr ->
                userLat = lat
                userLng = lng
                hasGpsLocation = true
                locationAddress = addr
            }
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Ensure Mapbox Access Token is initialized
    LaunchedEffect(mapboxToken) {
        val tokenToUse = mapboxToken.ifBlank { BuildConfig.MAPBOX_ACCESS_TOKEN }
        if (tokenToUse.isNotBlank() && tokenToUse.startsWith("pk.")) {
            MapboxOptions.accessToken = tokenToUse
            Log.d("MapboxDebug", "Token successfully set: ${tokenToUse.take(12)}...")
        } else {
            mapLoadError = "Invalid or missing Mapbox Access Token"
            Log.e("MapboxDebug", "Invalid Mapbox token: $tokenToUse")
        }
    }

    // Prepare clinics with live distances
    val displayClinics = remember(clinics, userLat, userLng) {
        clinics.map { clinic ->
            val dist = calculateDistanceKm(userLat, userLng, clinic.latitude ?: 0.0, clinic.longitude ?: 0.0)
            DisplayClinic(
                id = clinic.id.toString(),
                name = clinic.name,
                distanceKm = dist,
                openHours = clinic.openHours,
                address = clinic.address,
                phone = clinic.phone,
                isNearest = false,
                latitude = clinic.latitude ?: -25.7479,
                longitude = clinic.longitude ?: 28.1881
            )
        }.sortedBy { it.distanceKm }
    }

    // Filtered clinics for search
    val filteredClinics = remember(displayClinics, searchQuery) {
        if (searchQuery.isBlank()) displayClinics
        else displayClinics.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true)
        }
    }

    // Mapbox Viewport State
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(userLng, userLat))
            zoom(12.5)
            pitch(0.0)
            bearing(0.0)
        }
    }

    // Recenter map when GPS acquires
    LaunchedEffect(userLat, userLng, hasGpsLocation) {
        if (hasGpsLocation) {
            mapViewportState.flyTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(userLng, userLat))
                    .zoom(13.5)
                    .build()
            )
        }
    }

    // Create marker bitmap for clinic pins
    val clinicPinBitmap = remember {
        createCustomMarkerBitmap(color = 0xFF0D9488.toInt(), text = "H")
    }
    val userPinBitmap = remember {
        createCustomMarkerBitmap(color = 0xFF2563EB.toInt(), text = "You")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // --- SEARCH BAR ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clinic_search_input"),
            placeholder = { Text("Search clinics, hospitals, pharmacies...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TealPrimary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- MAP CONTAINER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isMapExpanded) 1f else 0.55f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2E3D35))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E293B))
            ) {
                // Native Mapbox Compose Map
                MapboxMap(
                    modifier = Modifier.fillMaxSize(),
                    mapViewportState = mapViewportState,
                    // FIX 1: Provide TextureView for reliable emulator/streaming compositing
                    composeMapInitOptions = remember {
                        ComposeMapInitOptions(
                            pixelRatio = density.density,
                            textureView = true
                        )
                    },
                    // FIX 2: Explicit Style definition so tiles and vector layers render
                    style = {
                        GenericStyle(style = Style.MAPBOX_STREETS)
                    }
                ) {
                    // FIX 3: User Location Pin with visible icon
                    PointAnnotation(
                        point = Point.fromLngLat(userLng, userLat)
                    ) {
                        iconImage = IconImage(userPinBitmap)
                    }

                    // FIX 4: Clinic Pins with visible icons & circle anchors
                    filteredClinics.forEach { clinic ->
                        PointAnnotation(
                            point = Point.fromLngLat(clinic.longitude, clinic.latitude)
                        ) {
                            iconImage = IconImage(clinicPinBitmap)
                            interactionsState = interactionsState.onClicked {
                                selectedClinicId = clinic.id
                                true
                            }
                        }
                        CircleAnnotation(
                            point = Point.fromLngLat(clinic.longitude, clinic.latitude)
                        ) {
                            circleRadius = 8.0
                            circleColor = if (selectedClinicId == clinic.id) Color(0xFFDC2626) else Color(0xFF0D9488)
                            circleOpacity = 0.85
                        }
                    }
                }

                // Controls: Recenter & Fullscreen
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            mapViewportState.flyTo(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(userLng, userLat))
                                    .zoom(14.0)
                                    .build()
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Recenter", tint = TealPrimary)
                    }
                    SmallFloatingActionButton(
                        onClick = { isMapExpanded = !isMapExpanded },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(
                            if (isMapExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Fullscreen",
                            tint = TealPrimary
                        )
                    }
                }
            }
        }

        // --- ACTIVE NAVIGATION PANEL ---
        if (activeRoute != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedDestination?.name ?: "Active Route",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${activeRoute.formattedDuration} (${activeRoute.formattedDistance})",
                                fontSize = 12.sp,
                                color = TealPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = {
                            isSimulatingRoute = false
                            onClearRoute()
                            onClearDestination()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Route")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val lat = selectedDestination?.latitude ?: 0.0
                                val lng = selectedDestination?.longitude ?: 0.0
                                val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open in Phone GPS", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- CLINICS LIST ---
        if (!isMapExpanded && activeRoute == null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Nearby Health Facilities (${filteredClinics.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredClinics) { clinic ->
                    ClinicItemCard(
                        clinic = clinic,
                        isSelected = clinic.id == selectedClinicId,
                        onSelect = {
                            selectedClinicId = clinic.id
                            mapViewportState.flyTo(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(clinic.longitude, clinic.latitude))
                                    .zoom(14.5)
                                    .build()
                            )
                        },
                        onGetDirections = {
                            onGetDirections(clinic, routeProfile, userLat, userLng)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ClinicItemCard(
    clinic: DisplayClinic,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onGetDirections: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(1.dp, TealPrimary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(clinic.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(clinic.address, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", clinic.distanceKm)} km away",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(clinic.openHours, fontSize = 11.sp, color = Color.Gray)
                }
            }

            Button(
                onClick = onGetDirections,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Route", fontSize = 11.sp)
            }
        }
    }
}

// Helper to generate circular marker bitmaps dynamically
private fun createCustomMarkerBitmap(color: Int, text: String): Bitmap {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    // Circle background
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

    // White border
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, strokePaint)

    // Text symbol
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    canvas.drawText(text, size / 2f, size / 2f + 8, textPaint)
    return bitmap
}

@SuppressLint("MissingPermission")
private fun fetchDeviceLocation(
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    onSuccess: (Double, Double, String) -> Unit
) {
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { loc ->
            if (loc != null) {
                var addressText = "Current Location"
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val list = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    if (!list.isNullOrEmpty()) {
                        addressText = list[0].locality ?: list[0].subAdminArea ?: list[0].adminArea ?: addressText
                    }
                } catch (_: Throwable) {}
                onSuccess(loc.latitude, loc.longitude, addressText)
            }
        }
}
