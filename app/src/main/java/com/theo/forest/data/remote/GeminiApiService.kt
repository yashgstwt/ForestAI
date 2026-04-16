package com.theo.forest.data.remote

import com.theo.forest.data.modal.GeminiRequest
import com.theo.forest.data.modal.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {

    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun getDiseaseInfo(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

}