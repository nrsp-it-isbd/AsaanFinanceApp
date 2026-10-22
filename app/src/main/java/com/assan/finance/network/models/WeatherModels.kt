package com.assan.finance.network.models

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val hourly: HourlyWeather,
    val daily: DailyWeather
)

data class HourlyWeather(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val weathercode: List<Int>,
    val precipitation_probability: List<Int>
)

data class DailyWeather(
    val time: List<String>,
    val weathercode: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_probability_max: List<Int>
)
