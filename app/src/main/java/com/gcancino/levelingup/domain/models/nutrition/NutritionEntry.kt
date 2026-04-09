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

    // What the LLM identified
    val foodIdentified: String,
    val photoUrl: String = "",     // uploaded to Firebase Storage

    // Macros — always computed with Atwater in Python, not LLM
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatsG: Float,
    val fiberG: Float,

    // Micronutrients
    val microHighlights: List<String> = emptyList(),
    val microConcerns: List<String>   = emptyList(),

    // Processing level
    val processingLevel: ProcessingLevel = ProcessingLevel.UNKNOWN,

    // Identity framing — the core differentiator
    val alignment: NutritionAlignment,
    val alignmentReason: String,
    val suggestion: String,

    val isSynced: Boolean = false
) {
    // Daily macros helper for the nutrition dashboard
    val macroSummary get() = MacroSummary(calories, proteinG, carbsG, fatsG, fiberG)
}
