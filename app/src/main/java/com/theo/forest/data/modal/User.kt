package com.theo.forest.data.modal

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
)
