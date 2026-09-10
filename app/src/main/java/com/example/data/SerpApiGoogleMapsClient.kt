package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Model representing a place fetched live from Google Maps via SerpApi:
 * https://serpapi.com/search?engine=google_maps
 */
data class SerpApiPlace(
    val title: String,
    val address: String,
    val phone: String,
    val openHours: String,
    val rating: Double,
    val reviewsCount: Int,
    val placeType: String,
    val latitude: Double?,
    val longitude: Double?,
    val thumbnail: String?,
    val website: String?,
    val directionsUrl: String?,
    val placeId: String?
) {
    /**
     * Converts to Room's ClinicEntity for caching, offline usage, and unified display.
     */
    fun toClinicEntity(index: Int, originLat: Double = -25.75, originLng: Double = 28.22): ClinicEntity {
        val calculatedDist = if (latitude != null && longitude != null) {
            calculateDistanceKm(originLat, originLng, latitude, longitude)
        } else {
            1.2 + (index * 0.9)
        }

        val latOff = if (latitude != null) {
            ((latitude - originLat) * 1200).toFloat().coerceIn(-85f, 85f)
        } else {
            (index * 30f - 45f)
        }

        val lngOff = if (longitude != null) {
            ((longitude - originLng) * 1200).toFloat().coerceIn(-85f, 85f)
        } else {
            (index * 30f - 45f)
        }

        return ClinicEntity(
            id = 0,
            name = title,
            distanceKm = Math.round(calculatedDist * 10.0) / 10.0,
            openHours = openHours.ifBlank { "Open today" },
            address = address.ifBlank { "Address listed on Google Maps" },
            phone = phone.ifBlank { "Available on Google Maps" },
            isNearest = (index == 0),
            latOffsetDp = latOff,
            lngOffsetDp = lngOff
        )
    }
}

sealed class SerpApiSearchResult {
    data class Success(
        val places: List<SerpApiPlace>,
        val query: String,
        val totalResults: Int
    ) : SerpApiSearchResult()

    data class Error(
        val message: String,
        val isApiKeyMissing: Boolean = false
    ) : SerpApiSearchResult()
}

/**
 * Client for SerpApi's Google Maps Engine.
 * Documentation: https://serpapi.com/search?engine=google_maps
 */
class SerpApiGoogleMapsClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getInjectedApiKey(): String {
        return try {
            val key = BuildConfig.SERPAPI_API_KEY
            if (key.isNullOrBlank() || key == "MY_SERPAPI_KEY" || key == "\"MY_SERPAPI_KEY\"" || key == "MY_NEW_API_KEY_DEFAULT_VALUE") "" else key.trim()
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Executes a Google Maps search via SerpApi engine:
     * GET https://serpapi.com/search.json?engine=google_maps&q={query}&api_key={key}
     */
    suspend fun searchGoogleMaps(
        query: String,
        customApiKey: String? = null,
        centerLat: Double? = -25.7479,
        centerLng: Double? = 28.2293,
        zoom: Int = 13,
        countryCode: String = "za"
    ): SerpApiSearchResult = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim().ifBlank { "clinics and health centres" }
        val apiKey = customApiKey?.trim()?.ifBlank { null } ?: getInjectedApiKey()

        if (apiKey.isBlank()) {
            return@withContext SerpApiSearchResult.Error(
                message = "SerpApi API key not detected. Enter your SerpApi key to search live Google Maps.",
                isApiKeyMissing = true
            )
        }

        try {
            val encodedQuery = URLEncoder.encode(trimmedQuery, "UTF-8")
            val urlBuilder = StringBuilder("https://serpapi.com/search.json?")
            urlBuilder.append("engine=google_maps")
            urlBuilder.append("&type=search")
            urlBuilder.append("&q=").append(encodedQuery)
            urlBuilder.append("&api_key=").append(apiKey)
            urlBuilder.append("&hl=en")
            if (countryCode.isNotBlank()) {
                urlBuilder.append("&gl=").append(countryCode)
            }

            // If center coordinates are provided, format @latitude,longitude,zoom
            if (centerLat != null && centerLng != null) {
                urlBuilder.append("&ll=@${centerLat},${centerLng},${zoom}z")
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("SerpApiClient", "SerpApi error: HTTP ${response.code} - $responseBody")
                val errMsg = try {
                    val json = JSONObject(responseBody)
                    json.optString("error", "HTTP error ${response.code} from SerpApi")
                } catch (e: Throwable) {
                    "HTTP ${response.code}: Unable to reach SerpApi Google Maps engine"
                }
                return@withContext SerpApiSearchResult.Error(
                    message = errMsg,
                    isApiKeyMissing = response.code == 401
                )
            }

            val root = JSONObject(responseBody)

            // Check for error in JSON payload
            if (root.has("error")) {
                val err = root.getString("error")
                return@withContext SerpApiSearchResult.Error(
                    message = err,
                    isApiKeyMissing = err.contains("api_key", ignoreCase = true)
                )
            }

            val localResults = root.optJSONArray("local_results")
            if (localResults == null || localResults.length() == 0) {
                return@withContext SerpApiSearchResult.Success(
                    places = emptyList(),
                    query = trimmedQuery,
                    totalResults = 0
                )
            }

            val places = mutableListOf<SerpApiPlace>()
            for (i in 0 until localResults.length()) {
                val item = localResults.getJSONObject(i)
                val title = item.optString("title", "Medical Centre")
                val address = item.optString("address", "")
                val phone = item.optString("phone", "")
                val openHours = item.optString("open_state", item.optString("hours", "Hours not specified"))
                val rating = item.optDouble("rating", 0.0)
                val reviews = item.optInt("reviews", 0)
                val placeType = item.optString("type", "Clinic")
                val thumbnail = item.optString("thumbnail", "")
                val website = item.optString("website", "")
                val placeId = item.optString("place_id", "")

                var lat: Double? = null
                var lng: Double? = null
                val coords = item.optJSONObject("gps_coordinates")
                if (coords != null) {
                    lat = coords.optDouble("latitude")
                    lng = coords.optDouble("longitude")
                    if (lat.isNaN() || lng.isNaN()) {
                        lat = null
                        lng = null
                    }
                }

                // Directions Link
                val linksObj = item.optJSONObject("links")
                val directions = linksObj?.optString("directions") ?: if (lat != null && lng != null) {
                    "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
                } else {
                    "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode("$title $address", "UTF-8")}"
                }

                places.add(
                    SerpApiPlace(
                        title = title,
                        address = address,
                        phone = phone,
                        openHours = openHours,
                        rating = rating,
                        reviewsCount = reviews,
                        placeType = placeType,
                        latitude = lat,
                        longitude = lng,
                        thumbnail = thumbnail,
                        website = website,
                        directionsUrl = directions,
                        placeId = placeId
                    )
                )
            }

            SerpApiSearchResult.Success(
                places = places,
                query = trimmedQuery,
                totalResults = places.size
            )
        } catch (e: Throwable) {
            Log.e("SerpApiClient", "Exception querying SerpApi Google Maps", e)
            SerpApiSearchResult.Error("Network error querying Google Maps: ${e.message ?: "Unknown error"}")
        }
    }
}

/**
 * Calculates distance between two GPS coordinates using Haversine formula in kilometers.
 */
fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Radius of the Earth in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
