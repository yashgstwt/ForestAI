package com.theo.forest.data.modal

sealed class Response<out T> {
    data class Success<T>(val result : T) : Response<T>()
    data class Error(val error : String ) : Response<Nothing>()
    data object Loading : Response<Nothing>()
}