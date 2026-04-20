package com.gcancino.levelingup.domain.models.nutrition

data class TodayNutritionData(
    val entries: List<NutritionEntry> = emptyList(),
    val macros: MacroSummary = MacroSummary(0, 0f, 0f, 0f, 0f)
)
