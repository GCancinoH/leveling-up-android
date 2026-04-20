package com.gcancino.levelingup.domain.models.identity

import kotlinx.serialization.Serializable

@Serializable
enum class StandardType {
    // Physical & Vitality
    TRAINING,
    NUTRITION,
    SLEEP,
    // Mental & Discipline
    MINDSET,
    LEARNING,
    DEEP_WORK,
    // Character & Social
    FINANCE,
    // Catch-all
    CUSTOM
}