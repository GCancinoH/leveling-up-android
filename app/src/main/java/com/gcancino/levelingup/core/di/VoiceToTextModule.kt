package com.gcancino.levelingup.core.di

import android.content.Context
import com.gcancino.levelingup.core.IVoiceToTextParser
import com.gcancino.levelingup.core.OnlineVoiceParser
import com.gcancino.levelingup.core.voiceParser.OfflineVoiceParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.vosk.Model
import java.io.IOException
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object VoiceToTextModule {

    private const val VOSK_MODEL_PATH = "model-en-us"

    @Provides
    @Singleton
    fun provideVoskModel(@ApplicationContext context: Context): Model? {
        return try {
            Model(context.assets, VOSK_MODEL_PATH)
        } catch (e: IOException) {
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
