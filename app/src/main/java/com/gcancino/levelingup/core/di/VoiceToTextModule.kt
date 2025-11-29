package com.gcancino.levelingup.core.di

import android.content.Context
import com.gcancino.levelingup.core.IVoiceToTextParser
import com.gcancino.levelingup.core.OnlineVoiceParser
import com.gcancino.levelingup.core.voiceParser.OfflineVoiceParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import com.gcancino.levelingup.utils.AssetsUtil
import dagger.hilt.components.SingletonComponent
import org.vosk.Model
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object VoiceToTextModule {

    private const val VOSK_MODEL_PATH = "model-en-us"

    @Provides
    @Singleton
    fun provideVoskModel(@ApplicationContext context: Context): Model? {
        val modelPath = AssetsUtil.unpackAssetsFolder(context, VOSK_MODEL_PATH)
        return if (modelPath != null) {
            try {
                Model(modelPath)
            } catch (e: Exception) {
                null
            }
        } else {
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
