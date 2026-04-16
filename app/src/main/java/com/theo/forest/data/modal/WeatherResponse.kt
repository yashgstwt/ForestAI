package com.theo.forest.data.modal

import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val address: String,
    val days: List<WeatherDay>
)

@Serializable
data class WeatherDay(
    val datetime: String,
    val tempmax: Double,
    val tempmin: Double,
    val temp: Double,
    val humidity: Double,
    val conditions: String,
    val icon: String,
    val description: String? = null
)
