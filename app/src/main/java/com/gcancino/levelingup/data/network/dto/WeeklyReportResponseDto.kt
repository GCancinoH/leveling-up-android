package com.gcancino.levelingup.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WeeklyReportResponseDto(
    @SerialName("overall_score") val overallScore: Float,
    val headline: String,
    @SerialName("strongest_role") val strongestRole: String,
    @SerialName("weakest_role") val weakestRole: String,
    @SerialName("pattern_identified") val patternIdentified: String,
    @SerialName("mirror_insight") val mirrorInsight: String,
    @SerialName("one_correction") val oneCorrection: String,
    @SerialName("identity_alignment") val identityAlignment: String,
    @SerialName("generated_quest") val generatedQuest: JsonElement? = null
)
