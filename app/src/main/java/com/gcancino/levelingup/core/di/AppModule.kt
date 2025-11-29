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
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /*@Provides
    @Singleton
    fun provideVoskModel(@ApplicationContext context: Context): Model {
        val modelDir = File(context.filesDir, "model")

        // Check if model is already unpacked
        if (!modelDir.exists()) {
            // Unpack synchronously using a blocking approach
            val latch = CountDownLatch(1)
            var modelInstance: Model? = null
            var error: IOException? = null

            StorageService.unpack(
                context,
                "model-en-us",
                "model",
                { model: Model? ->
                    modelInstance = model
                    Timber.tag("Vosk").d("Model loaded successfully")
                    latch.countDown()
                },
                { exception: IOException? ->
                    error = exception
                    Timber.tag("Vosk").e("Failed to unpack model: ${exception?.message}")
                    latch.countDown()
                }
            )

            // Wait for unpacking to complete
            latch.await()

            if (error != null) {
                throw IOException("Failed to create Vosk model", error)
            }
        }

        // Return the model after ensuring it's unpacked
        return Model(modelDir.absolutePath)
    }*/
}
