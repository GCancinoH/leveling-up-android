package com.gcancino.levelingup.core.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

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
