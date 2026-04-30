package com.theo.forest.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.content
import com.google.gson.Gson
import com.theo.forest.data.modal.*
import com.theo.forest.data.remote.WeatherApiService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

class ApiRepository @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val weatherApiService: WeatherApiService,
    private val supabase: SupabaseClient
) {

    suspend fun saveScanResult(bitmap: Bitmap, mlResult: MLResult, info: DiseaseInfo?): Response<Unit> {
        return try {
            Log.d("Supabase", "Starting saveScanResult")
            val fileName = "${UUID.randomUUID()}.jpg"
            val bucket = supabase.storage.from("scans")

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val byteArray = baos.toByteArray()

            Log.d("Supabase", "Uploading image: $fileName")
            bucket.upload(fileName, byteArray)
            val imageUrl = bucket.publicUrl(fileName)
            Log.d("Supabase", "Image uploaded: $imageUrl")

            val record = ScanRecord(
                disease = mlResult.disease,
                confidence = mlResult.confidence,
                image_url = imageUrl,
                description = info?.description,
                symptoms = info?.symptoms,
                causes = info?.causes,
                treatment = info?.treatment,
                prevention = info?.prevention
            )

            Log.d("Supabase", "Inserting record: $record")
            supabase.postgrest.from("scans").insert(record)
            Log.d("Supabase", "Record inserted successfully")
            Response.Success(Unit)
        } catch (e: Exception) {
            Log.e("Supabase", "Error saving scan: ${e.message}", e)
            Response.Error(e.localizedMessage ?: "Failed to save to Supabase")
        }
    }

    suspend fun getWeatherData(location: String, apiKey: String): Response<WeatherResponse> {
        return try {
            val response = weatherApiService.getNext7DaysForecast(location, apiKey = apiKey)
            Response.Success(response)
        } catch (e: Exception) {
            Response.Error(e.localizedMessage ?: "Failed to fetch weather data")
        }
    }

    suspend fun getDiseaseInfo(diseaseName: String): Response<DiseaseInfo> {
        val prompt = """
            Provide detailed information about the plant disease: $diseaseName.
            Return ONLY a JSON object with the following keys:
            "description", "symptoms", "causes", "treatment", "prevention".
            
            IMPORTANT: All values MUST be simple strings (plain text paragraphs). 
            Do NOT use nested objects, lists, or arrays within the JSON values.
            Example format:
            {
              "description": "...",
              "symptoms": "...",
              "causes": "...",
              "treatment": "...",
              "prevention": "..."
            }
        """.trimIndent()

        return callGemini(prompt)
    }

    suspend fun getGeminiPrediction(bitmap: android.graphics.Bitmap): Response<Pair<MLResult, DiseaseInfo>> {
        val prompt = """
            Analyze this plant leaf image. 
            First, identify the disease or if it is healthy.
            Then, provide detailed information.
            
            Return ONLY a JSON object with two parts:
            1. "prediction": { "disease": "disease name", "confidence": 0.95 }
            2. "info": { "description": "...", "symptoms": "...", "causes": "...", "treatment": "...", "prevention": "..." }
            
            If it's just a background or not a leaf, set disease to "Background".
            Confidence should be a float between 0 and 1.
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
            val jsonString = extractJson(response.text)
            if (jsonString != null) {
                val root = Gson().fromJson(jsonString, com.google.gson.JsonObject::class.java)
                val predictionJson = root.getAsJsonObject("prediction")
                val infoJson = root.getAsJsonObject("info")
                
                val mlResult = MLResult(
                    disease = predictionJson.get("disease").asString,
                    confidence = predictionJson.get("confidence").asFloat
                )
                val diseaseInfo = Gson().fromJson(infoJson, DiseaseInfo::class.java)
                
                Response.Success(Pair(mlResult, diseaseInfo))
            } else {
                Response.Error("Invalid AI response")
            }
        } catch (e: Exception) {
            Response.Error(e.localizedMessage ?: "Gemini Analysis Failed")
        }
    }

    private fun extractJson(text: String?): String? {
        if (text == null) return null
        val startIndex = text.indexOf("{")
        val endIndex = text.lastIndexOf("}")
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            text.substring(startIndex, endIndex + 1)
        } else null
    }

    private suspend fun callGemini(prompt: String): Response<DiseaseInfo> {
        var retryCount = 0
        val maxRetries = 3
        var lastException: Exception? = null

        while (retryCount < maxRetries) {
            try {
                val response = generativeModel.generateContent(prompt)
                val jsonString = extractJson(response.text)

                if (jsonString != null) {
                    return try {
                        val diseaseInfo = Gson().fromJson(jsonString, DiseaseInfo::class.java)
                        Response.Success(diseaseInfo)
                    } catch (e: Exception) {
                        Log.e("TFLite", "JSON Parsing Error: ${e.message} for string: $jsonString")
                        Response.Error("Failed to parse disease information")
                    }
                } else {
                    return Response.Error("Empty response from AI")
                }
            } catch (e: Exception) {
                lastException = e
                val errorMessage = e.message ?: ""
                if (errorMessage.contains("high demand", ignoreCase = true) || 
                    errorMessage.contains("429", ignoreCase = true)) {
                    retryCount++
                    val waitTime = (1000L * (retryCount * 2))
                    delay(waitTime)
                } else {
                    break
                }
            }
        }
        return Response.Error(lastException?.localizedMessage ?: "Gemini Error")
    }
}
