package com.gcancino.levelingup.domain.models.identity

import kotlinx.serialization.Serializable

@Serializable
enum class TimeHorizon {
    DAY,
    WEEK,
    MONTH,
    QUARTER,
    YEAR
}
