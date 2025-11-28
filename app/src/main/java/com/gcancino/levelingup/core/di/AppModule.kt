package com.gcancino.levelingup.core.di

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.vosk.Model
import org.vosk.android.StorageService
import timber.log.Timber
import java.io.IOException
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton // Use @Singleton to ensure only one instance of the model is created
    fun provideVoskModel(@ApplicationContext context: Context): Model {
        val assetModelPath = "assets/model"
        val modelName = "model"

        StorageService.unpack(
            context, "model-en-us", "model",
            { model: Model? ->
                Timber.tag("Vosk").d("Model loaded successfully")
            },
            { exception: IOException? -> Timber.tag("Vosk").e("Failed to unpack the model%s", exception!!.message) })

        // Step 3: Instantiate the Model with the correct file path.
        return Model(assetModelPath)
    }
}
