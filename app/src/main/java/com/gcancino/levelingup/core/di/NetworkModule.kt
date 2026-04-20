package com.gcancino.levelingup.core.di

import com.gcancino.levelingup.core.Config
import com.gcancino.levelingup.core.network.AuthInterceptor
import com.gcancino.levelingup.data.network.IdentityApiService
import com.gcancino.levelingup.data.network.NutritionApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor = AuthInterceptor()

    @Provides
    @Singleton
    @Named("BaseOkHttp")
    fun provideBaseOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("VisionOkHttp")
    fun provideVisionOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS) // Vision models take longer
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        @Named("BaseOkHttp") okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(Config.BACKEND_ENDPOINT + "/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideNutritionApiService(retrofit: Retrofit): NutritionApiService {
        return retrofit.create(NutritionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideIdentityApiService(retrofit: Retrofit): IdentityApiService {
        return retrofit.create(IdentityApiService::class.java)
    }
}
