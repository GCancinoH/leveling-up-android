package com.gcancino.levelingup.domain.models.exercise

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ExerciseRole(val value: String) {
    @SerialName("Main Lift") MAIN("Main Lift"),
    @SerialName("Accessory") ACCESSORY("Accessory"),
    @SerialName("Complementary") COMPLEMENTARY("Complementary"),
    @SerialName("Corrective") CORRECTIVE("Corrective")
}
