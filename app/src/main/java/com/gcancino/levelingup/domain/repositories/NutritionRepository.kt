package com.gcancino.levelingup.domain.repositories

import android.net.Uri
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.nutrition.MacroSummary
import com.gcancino.levelingup.domain.models.nutrition.NutritionEntry
import kotlinx.coroutines.flow.Flow

interface NutritionRepository {
    suspend fun analyzeFood(
        uID: String,
        imageUri: Uri,
        identityStatement: String,
        nutritionStandardTitles: List<String>
    ): Resource<NutritionEntry>

    fun observeTodayEntries(uID: String): Flow<List<NutritionEntry>>
    fun observeTodayMacros(uID: String): Flow<MacroSummary>
    fun observeAlignedCountToday(uID: String): Flow<Int>
    fun observeRecentEntries(uID: String): Flow<List<NutritionEntry>>
    suspend fun syncUnsynced(): Resource<Unit>
}