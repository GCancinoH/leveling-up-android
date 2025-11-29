package com.gcancino.levelingup.core.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gcancino.levelingup.core.IVoiceToTextParser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named

interface IVoiceParserFactory {
    fun create(): IVoiceToTextParser?
}

class VoiceParserFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Named("online") private val onlineParser: IVoiceToTextParser,
    @param:Named("offline") private val offlineParser: IVoiceToTextParser?
) : IVoiceParserFactory {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun create(): IVoiceToTextParser? {
        return if (isNetworkAvailable()) {
            onlineParser
        } else {
            offlineParser
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
