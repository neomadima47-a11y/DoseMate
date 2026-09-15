package com.example.data

data class MapboxPlaceResult(
    val name: String,
    val placeName: String,
    val latitude: Double,
    val longitude: Double
)

data class MapboxRoute(
    val coordinates: List<Pair<Double, Double>>,
    val formattedDuration: String,
    val formattedDistance: String
)
