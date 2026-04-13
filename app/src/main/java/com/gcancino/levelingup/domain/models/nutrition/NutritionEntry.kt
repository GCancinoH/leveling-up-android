package com.gcancino.levelingup.domain.models.nutrition

import com.gcancino.levelingup.core.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class NutritionEntry(
    val id: String,
    val uID: String,
    @Serializable(with = DateSerializer::class)
    val date: Date,
    val foodIdentified: String,
    val photoUrl: String = "",
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatsG: Float,
    val fiberG: Float,
    val microHighlights: List<String>    = emptyList(),
    val microConcerns: List<String>      = emptyList(),
    val processingLevel: ProcessingLevel = ProcessingLevel.UNKNOWN,
    val alignment: NutritionAlignment,
    val alignmentReason: String,
    val suggestion: String,
    val alignmentScore: Float = 0f,      // 0.0–1.0, calculado en Python
    val action: NutritionAction? = null,
    val isFailed: Boolean = false,
    val isSynced: Boolean = false
) {
    val macroSummary get() = MacroSummary(calories, proteinG, carbsG, fatsG, fiberG)
}