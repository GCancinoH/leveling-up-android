package com.gcancino.levelingup.domain.models.exercise

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BlockType {
    @SerialName("Main") Main,
    @SerialName("SuperSet") SuperSet,
    @SerialName("JumpSet") JumpSet,
    @SerialName("Bi TuT") BiTuT,
    @SerialName("Circuit") Circuit,
    @SerialName("Warmup") Warmup,
    @SerialName("Cooldown") Cooldown
}
