package com.gcancino.levelingup.core

import timber.log.Timber
import com.gcancino.levelingup.BuildConfig

object Config {
    private const val TAG = "Config"

    @Volatile
    private var backendEndpointOverride: String? = null

    val DEFAULT_BACKEND_ENDPOINT: String = BuildConfig.DEFAULT_BACKEND_ENDPOINT

    val BACKEND_ENDPOINT: String
        get() = backendEndpointOverride ?: DEFAULT_BACKEND_ENDPOINT

    fun setBackendEndpointOverride(endpoint: String?) {
        if (BuildConfig.DEBUG) {
            backendEndpointOverride = endpoint
            Timber.tag(TAG).d("Backend endpoint override set: $endpoint")
        } else {
            Timber.tag(TAG).w("Attempted to override backend endpoint in release build.")
        }
    }

    fun resetBackendEndpointOverride() {
        backendEndpointOverride = null
    }
}