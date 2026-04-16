package com.theo.forest.data.modal

data class DiseaseInfo(
    val description: String,
    val symptoms: String,
    val causes: String,
    val treatment: String,
    val prevention: String
)

// Wrapper for Gemini API Structure
data class GeminiRequest(
    val contents: List<ContentRequest>
)

data class ContentRequest(
    val parts: List<PartRequest>
)

data class PartRequest(
    val text: String
)

// Wrapper for Gemini API Response
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: ContentResponse? = null
)

data class ContentResponse(
    val parts: List<PartResponse>? = null
)

data class PartResponse(
    val text: String? = null
)