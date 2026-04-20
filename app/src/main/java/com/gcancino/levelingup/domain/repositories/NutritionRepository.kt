package com.gcancino.levelingup.domain.repositories

import android.net.Uri
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.nutrition.MacroSummary
import com.gcancino.levelingup.domain.models.nutrition.NutritionEntry
import com.gcancino.levelingup.domain.models.nutrition.NutritionStandardDto
import com.gcancino.levelingup.domain.models.nutrition.TodayNutritionData
import kotlinx.coroutines.flow.Flow

interface NutritionRepository {
    suspend fun analyzeFood(
        uID: String,
        imageUri: Uri,
        identityStatement: String,
        nutritionStandards: List<NutritionStandardDto>
    ): Resource<NutritionEntry>
    fun observeTodayData(uID: String): Flow<TodayNutritionData>
    fun observeAlignedCountToday(uID: String): Flow<Int>
    fun observeRecentEntries(uID: String): Flow<List<NutritionEntry>>
    suspend fun syncUnsynced(): Resource<Unit>
}