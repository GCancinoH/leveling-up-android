package com.gcancino.levelingup.domain.models.nutrition

import kotlinx.serialization.Serializable

@Serializable
data class NutritionAction(
    val type: NutritionActionType,
    val taskTitle: String? = null,  // si type == ADD_TASK
    val standardId: String? = null,  // estándar NUTRITION violado
    val message: String? = null   // si type == WARNING
)

enum class NutritionActionType {
    ADD_TASK,
    WARNING,
    NONE
}