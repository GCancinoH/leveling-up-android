package com.gcancino.levelingup.domain.models.identity

import kotlinx.serialization.Serializable

@Serializable
enum class ObjectiveStatus {
    ACTIVE,
    COMPLETED,
    DROPPED,
    ARCHIVED
}
