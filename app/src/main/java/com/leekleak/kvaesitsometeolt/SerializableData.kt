package com.leekleak.kvaesitsometeolt

import kotlinx.serialization.Serializable

@Serializable
data class LocationForecast (
    var place: LocationMeteoLT,
    var forecastType: String,
    var forecastCreationTimeUtc: String,
    var forecastTimestamps: List<ForecastTimestamp>
)

@Serializable
data class ForecastTimestamp (
    var forecastTimeUtc: String,
    var airTemperature: Double,
    var windSpeed: Double,
    var windDirection: Double,
    var cloudCover: Int,
    var seaLevelPressure: Double,
    var relativeHumidity: Int,
    var totalPrecipitation: Double,
    var conditionCode: String
)

@Serializable
data class LocationMeteoLT (
    var code: String,
    var name: String,
    var administrativeDivision: String,
    var coordinates: Coordinates
)

@Serializable
data class Coordinates (
    var latitude: Double,
    var longitude: Double
)