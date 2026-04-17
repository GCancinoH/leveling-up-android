package com.gcancino.levelingup.core

import android.util.Log

object Config {
    private const val TAG = "Config"

    @Volatile
    private var backendEndpointOverride: String? = null

    const val DEFAULT_BACKEND_ENDPOINT = "https://leveling-up-server.vercel.app"

    val BACKEND_ENDPOINT: String
        get() = backendEndpointOverride ?: DEFAULT_BACKEND_ENDPOINT

    fun setBackendEndpointOverride(endpoint: String?) {
        backendEndpointOverride = endpoint
        Log.d(TAG, "Backend endpoint override set: $endpoint")
    }

    fun resetBackendEndpointOverride() {
        backendEndpointOverride = null
    }
}