package com.leekleak.kvaesitsometeolt

import de.mm20.launcher2.plugin.config.WeatherPluginConfig
import de.mm20.launcher2.sdk.weather.C
import de.mm20.launcher2.sdk.weather.Forecast
import de.mm20.launcher2.sdk.weather.WeatherLocation
import de.mm20.launcher2.sdk.weather.WeatherProvider
import de.mm20.launcher2.sdk.weather.m_s
import de.mm20.launcher2.sdk.weather.mbar
import de.mm20.launcher2.sdk.weather.mm
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.shredzone.commons.suncalc.SunTimes
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.net.ssl.HttpsURLConnection


@OptIn(ExperimentalSerializationApi::class)
class KvaesitsoMeteoLT : WeatherProvider(
    WeatherPluginConfig(60000)
) {

    private val apiRoot = "https://api.meteo.lt/v1"
    private val parser = Json { ignoreUnknownKeys = true }
    private var locationCache: List<LocationMeteoLT> = emptyList()

    override suspend fun findLocations(query: String, lang: String): List<WeatherLocation> {
        return getAllLocationsMeteoLT()
            .filter { it.name.contains(query, true) }
            .map { WeatherLocation.Id(name = "${it.name}, ${it.administrativeDivision}", id = it.code) }
    }

    private fun getAllLocationsMeteoLT(): List<LocationMeteoLT> {
        if (locationCache.isEmpty()) {
            val connection: HttpsURLConnection = URL("$apiRoot/places").openConnection() as HttpsURLConnection
            connection.inputStream.use { response ->
                val locations = parser.decodeFromStream<List<LocationMeteoLT>>(response).onEach { it.administrativeDivision = shortenAdminDiv(it.administrativeDivision) }
                locationCache = locations
            }
        }
        return locationCache
    }

    private fun getBestLocation(lat: Double, lon: Double): WeatherLocation.Id {
        val locations = getAllLocationsMeteoLT()
        var bestLocation: LocationMeteoLT = locations.first()
        var smallestDistance = Long.MAX_VALUE

        for (l in locations) {
            val distance = distanceBetweenCoordinates(lat, lon, l.coordinates.latitude, l.coordinates.longitude)
            if (distance < smallestDistance) {
                smallestDistance = distance.toLong()
                bestLocation = l
            }
        }

        if (smallestDistance > 50000) error("User is too far to any forecast point")
        return WeatherLocation.Id(name = "${bestLocation.name}, ${bestLocation.administrativeDivision}", id = bestLocation.code)
    }

    private fun getLocationForecasts(location: WeatherLocation.Id): List<Forecast> {
        val forecastMeteo: LocationForecast
        val forecasts: MutableList<Forecast> = mutableListOf()

        //This assumes that the location has long-term forecasts. May be wrong in the future idk.
        val connection: HttpsURLConnection = URL("$apiRoot/places/${location.id}/forecasts/long-term").openConnection() as HttpsURLConnection

        connection.inputStream.use { response ->
            forecastMeteo = parser.decodeFromStream<LocationForecast>(response)
        }

        if (forecastMeteo.forecastTimestamps.isEmpty()) error("Did not recieve any forecasts from the site")

        val creationTime = parseTimestamp(forecastMeteo.forecastCreationTimeUtc)
        var lastDate: LocalDateTime = LocalDateTime.now()
        var savedSunTimes: SunTimes = createSunTime(lastDate, forecastMeteo)

        for (a in forecastMeteo.forecastTimestamps) {
            val timestampDate = parseTimestamp(a.forecastTimeUtc)

            if (timestampDate.dayOfYear != lastDate.dayOfYear) {
                savedSunTimes = createSunTime(timestampDate, forecastMeteo)
                lastDate = timestampDate
            }

            val isNight = timestampDate.hour <= roundLocalDateTime(savedSunTimes.rise!!).hour ||
                          timestampDate.hour >= roundLocalDateTime(savedSunTimes.set!!).hour

            forecasts.add(
                Forecast(
                    timestamp = timestampDate.toEpochSecond(ZoneOffset.UTC) * 1000,
                    createdAt = creationTime.toEpochSecond(ZoneOffset.UTC) * 1000,
                    temperature = a.airTemperature.C,
                    condition = parseConditionCode(a.conditionCode),
                    icon = parseConditionIcon(a.conditionCode),
                    night = isNight,
                    pressure = a.seaLevelPressure.mbar,
                    humidity = a.relativeHumidity,
                    windSpeed = a.windSpeed.m_s,
                    windDirection = a.windDirection,
                    precipitation = a.totalPrecipitation.mm,
                    clouds = a.cloudCover,
                    location = forecastMeteo.place.name,
                    provider = "Meteo.lt",
                    providerUrl = "https://meteo.lt/"
                )
            )
        }
        return forecasts
    }

    override suspend fun getWeatherData(lat: Double, lon: Double, lang: String?): List<Forecast> {
        return try {
            getLocationForecasts(getBestLocation(lat, lon))
        } catch (e: IllegalStateException) {
            emptyList()
        }

    }

    override suspend fun getWeatherData(location: WeatherLocation, lang: String?): List<Forecast> {
        return when (location) {
            is WeatherLocation.Id -> getLocationForecasts(WeatherLocation.Id(location.id, location.name)) // Bug Workaround
            else -> emptyList()
        }
    }
}