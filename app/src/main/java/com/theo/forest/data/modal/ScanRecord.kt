package com.theo.forest.data.modal

import kotlinx.serialization.Serializable

@Serializable
data class ScanRecord(
    val id: Int? = null,
    val disease: String,
    val confidence: Float,
    val image_url: String,
    val description: String? = null,
    val symptoms: String? = null,
    val causes: String? = null,
    val treatment: String? = null,
    val prevention: String? = null,
    val created_at: String? = null
)
