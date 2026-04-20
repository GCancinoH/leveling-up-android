package com.gcancino.levelingup.domain.models.nutrition

import kotlinx.serialization.Serializable

@Serializable
data class NutritionStandardDto(
    val id: String,    // real IdentityStandard.id
    val title: String  // IdentityStandard.title
)