package com.gcancino.levelingup.core

sealed class Resource<out T>(
    val data: T? = null,
    val message: String? = null,
    val exception: Exception? = null
) {
    class Success<out T>(data: T) : Resource<T>(data)
    class Error<out T>(message: String, data: T? = null, exception: Exception? = null) : Resource<T>(data, message, exception)
    class Loading<out T>(data: T? = null) : Resource<T>(data)
}