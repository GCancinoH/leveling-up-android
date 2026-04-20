package com.gcancino.levelingup.data.network

import com.gcancino.levelingup.data.network.dto.NutritionResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface NutritionApiService {
    @Multipart
    @POST("nutrition/analyze")
    suspend fun analyzeFood(
        @Part image: MultipartBody.Part,
        @Part("identity_statement") identityStatement: RequestBody,
        @Part("nutrition_standards") nutritionStandards: RequestBody
    ): NutritionResponseDto
}
