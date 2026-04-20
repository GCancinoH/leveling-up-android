package com.gcancino.levelingup.core.di.voiceParser

import android.content.Context
import com.gcancino.levelingup.core.IVoiceToTextParser
import com.gcancino.levelingup.core.OnlineVoiceParser
import com.gcancino.levelingup.core.voiceParser.OfflineVoiceParser
import com.gcancino.levelingup.utils.AssetsUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.vosk.Model
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceToTextModule {

    private const val VOSK_MODEL_PATH = "model-en-us"

    @Provides
    @Singleton
    fun provideVoskModel(@ApplicationContext context: Context): Model? {
        val modelPath = File(context.filesDir, VOSK_MODEL_PATH)
        val modelCheckFile = File(modelPath, "am/final.mdl")
        
        return if (modelCheckFile.exists()) {
            try {
                Model(modelPath.absolutePath)
            } catch (e: Exception) {
                Timber.tag("VoiceToTextModule").e(e, "Failed to initialize Vosk model")
                null
            }
        } else {
            Timber.tag("VoiceToTextModule").w("Vosk model not yet unpacked")
            null
        }
    }

    @Provides
    @Named("online")
    fun provideOnlineVoiceParser(@ApplicationContext context: Context): IVoiceToTextParser {
        return OnlineVoiceParser(context)
    }

    @Provides
    @Named("offline")
    fun provideOfflineVoiceParser(
        @ApplicationContext context: Context,
        model: Model?
    ): IVoiceToTextParser? {
        return model?.let { OfflineVoiceParser(context, it) }
    }

}