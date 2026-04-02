package com.gcancino.levelingup.domain.models.exercise

import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class IntensityType(val value: String) {
    @PropertyName("RPE")
    @SerialName("RPE")
    RPE("RPE"),

    @PropertyName("RIR")
    @SerialName("RIR")
    RIR("RIR"),

    @PropertyName("% 1RM")
    @SerialName("% 1RM")
    PERCENTAGE_1RM("% 1RM")
}

