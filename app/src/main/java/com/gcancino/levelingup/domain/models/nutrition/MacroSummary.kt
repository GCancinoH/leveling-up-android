package com.gcancino.levelingup.domain.models.nutrition

import kotlinx.serialization.Serializable

@Serializable
data class MacroSummary(
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatsG: Float,
    val fiberG: Float
) {
    operator fun plus(other: MacroSummary) = MacroSummary(
        calories = this.calories + other.calories,
        proteinG = this.proteinG + other.proteinG,
        carbsG   = this.carbsG + other.carbsG,
        fatsG    = this.fatsG + other.fatsG,
        fiberG   = this.fiberG + other.fiberG
    )
}
