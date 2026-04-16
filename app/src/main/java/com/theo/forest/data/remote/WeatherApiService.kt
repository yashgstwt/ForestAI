package com.theo.forest.data.remote

import com.theo.forest.data.modal.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherApiService {
    @GET("VisualCrossingWebServices/rest/services/timeline/{location}/next7days")
    suspend fun getNext7DaysForecast(
        @Path("location") location: String,
        @Query("unitGroup") unitGroup: String = "metric",
        @Query("key") apiKey: String,
        @Query("contentType") contentType: String = "json"
    ): WeatherResponse
}
