package com.leekleak.kvaesitsometeolt

import de.mm20.launcher2.weather.WeatherIcon
import org.shredzone.commons.suncalc.SunTimes
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private fun rad(deg: Double): Double {
    return deg * PI / 180.0
}

fun shortenAdminDiv(data: String): String {
    return data
        .replace("miesto", "m.")
        .replace("rajono", "r.")
        .replace("savivaldybė", "sav.")
}

fun distanceBetweenCoordinates(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    //Return distance in meters
    return 12742000 * asin(sqrt(sin((rad(lat2) - rad(lat1))/2).pow(2) + cos(rad(lat1)) * cos(rad(lat2)) * sin((rad(lon2)-rad(lon1))/2).pow(2)))
}

fun parseConditionIcon(conditionCode: String): WeatherIcon {
    return when (conditionCode) {
        "clear" -> WeatherIcon.Clear
        "partly-cloudy" -> WeatherIcon.PartlyCloudy
        "cloudy-with-sunny-intervals" -> WeatherIcon.BrokenClouds
        "cloudy" -> WeatherIcon.Cloudy
        "thunder" -> WeatherIcon.Thunderstorm
        "isolated-thunderstorms" -> WeatherIcon.ThunderstormWithRain
        "thunderstorms" -> WeatherIcon.HeavyThunderstorm
        "heavy-rain-with-thunderstorms" -> WeatherIcon.HeavyThunderstormWithRain
        "light-rain" -> WeatherIcon.Drizzle
        "rain" -> WeatherIcon.Showers
        "heavy-rain" -> WeatherIcon.Storm
        "light-sleet", "sleet" -> WeatherIcon.Sleet
        "freezing-rain" -> WeatherIcon.Clear
        "hail" -> WeatherIcon.Hail
        "light-snow", "snow", "heavy-snow" -> WeatherIcon.Snow
        "fog" -> WeatherIcon.Fog
        else -> WeatherIcon.Unknown
    }
}

fun parseConditionCode(code: String): String {
    return code
        // Avoid using the word "sunny" as this code is sometimes found on night forecasts
        .replace("cloudy-with-sunny-intervals", "Broken Clouds")
        .replace('-', ' ')
        .replaceFirstChar { it.uppercaseChar() }
}

fun parseTimestamp(data: String): LocalDateTime {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    formatter.withZone(ZoneOffset.UTC)
    return LocalDateTime.parse(data, formatter)
}

fun createSunTime(date: LocalDateTime, forecast: LocationForecast): SunTimes {
    return SunTimes.compute()
        .on(date)
        .timezone(ZoneOffset.UTC)
        .latitude(forecast.place.coordinates.latitude)
        .longitude(forecast.place.coordinates.longitude)
        .execute()
}

fun roundLocalDateTime(a: ZonedDateTime): ZonedDateTime {
    if (a.minute >= 30) a.plusHours(1)
    a.truncatedTo(ChronoUnit.HOURS)
    return a
}