package com.gcancino.levelingup.domain.models.player
import kotlinx.serialization.Serializable

@Serializable
enum class Improvement {
    STRENGTH,
    MOBILITY,
    SELF_DEVELOPMENT,
    RECOVERY,
    MENTAL_TOUGHNESS,
    ENDURANCE,
    FINANCE
}