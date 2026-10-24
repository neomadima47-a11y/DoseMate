package com.example.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ClinicEntity
import com.example.data.HealthReadingEntity
import com.example.data.MapboxRoute
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
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

@Composable
fun ClinicLocatorScreen(
    clinics: List<ClinicEntity> = emptyList(),
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
    var searchQuery by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun executeSearch(query: String) {
        searchQuery = query
        val q = query.trim()
        if (q.isNotBlank()) {
            val escaped = q.replace("'", "\\'")
            webViewRef?.evaluateJavascript("searchLocation(-25.7479, 28.2293, '$escaped')", null)
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                executeSearch(spokenText)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // --- SEARCH BAR WITH VOICE ASSISTANT ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("🔍 Search hospital, clinic, pharmacy...", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak facility or clinic name...")
                        }
                        voiceLauncher.launch(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Voice search is not supported on this device", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Text("🎙️", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = { executeSearch(searchQuery) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Search ➢", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- MEDICAL ASSISTANCE QUICK SEARCH CHIPS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chips = listOf(
                "🏥 Hospitals" to "Pretoria General Hospital",
                "🩺 Public Clinics" to "Sunnyside Community Clinic",
                "🏥 Private Hospitals" to "Netcare Unitas Hospital",
                "💊 Pharmacies" to "Dis-Chem Pharmacy",
                "🚨 Emergency" to "Steve Biko Hospital Emergency"
            )

            chips.forEach { (label, query) ->
                Surface(
                    onClick = { executeSearch(query) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- GOOGLE MAP WEBVIEW CONTAINER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            loadUrl("file:///android_asset/google_map.html")
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Floating Recenter Location Button
            Button(
                onClick = {
                    webViewRef?.evaluateJavascript("recenterUserLocation()", null)
                    Toast.makeText(context, "Centered on live location", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Text("📍 My Location", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealAccent)
            }
        }
    }
}
