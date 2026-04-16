package com.theo.forest.data.repository

import android.util.Log
import com.google.gson.Gson
import com.theo.forest.data.modal.*
import kotlinx.coroutines.delay
import javax.inject.Inject

class ApiRepository @Inject constructor(private val generativeModel: com.google.firebase.ai.GenerativeModel) {

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

        var retryCount = 0
        val maxRetries = 3
        var lastException: Exception? = null

        while (retryCount < maxRetries) {
            try {
                val response = generativeModel.generateContent(prompt)
                var jsonString = response.text

                if (jsonString != null) {
                    val startIndex = jsonString.indexOf("{")
                    val endIndex = jsonString.lastIndexOf("}")
                    
                    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                        jsonString = jsonString.substring(startIndex, endIndex + 1)
                    }

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
                    val waitTime = (1000L * (retryCount * 2)) // Exponential backoff: 2s, 4s, 6s...
                    Log.w("TFLite", "API high demand, retrying in ${waitTime}ms (Attempt $retryCount/$maxRetries)")
                    delay(waitTime)
                } else {
                    // For other errors, don't retry
                    break
                }
            }
        }

        Log.e("TFLite", "API Error after $retryCount retries: ${lastException?.message}", lastException)
        return Response.Error(lastException?.localizedMessage ?: "Maximum retries reached or unknown error")
    }
}
