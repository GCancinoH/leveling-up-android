package com.gcancino.levelingup.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NutritionResponseDto(
    @SerialName("food_identified") val foodIdentified: String,
    @SerialName("total_calories_computed") val totalCalories: Int,
    val macros: MacrosDto,
    val micros: MicrosDto,
    @SerialName("processing_level") val processingLevel: String,
    val alignment: String,
    @SerialName("alignment_reason") val alignmentReason: String,
    val suggestion: String,
    @SerialName("alignment_score") val alignmentScore: Float,
    val action: ActionDto? = null
)

@Serializable
data class MacrosDto(
    @SerialName("protein_g") val proteinG: Float,
    @SerialName("carbs_g") val carbsG: Float,
    @SerialName("fats_g") val fatsG: Float,
    @SerialName("fiber_g") val fiberG: Float
)

@Serializable
data class MicrosDto(
    val highlights: List<String>,
    val concerns: List<String>
)

@Serializable
data class ActionDto(
    val type: String,
    val payload: ActionPayloadDto? = null
)

@Serializable
data class ActionPayloadDto(
    @SerialName("task_title") val taskTitle: String? = null,
    @SerialName("standard_id") val standardId: String? = null,
    val message: String? = null
)
