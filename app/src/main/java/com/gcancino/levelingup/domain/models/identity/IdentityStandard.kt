package com.gcancino.levelingup.domain.models.identity

import kotlinx.serialization.Serializable

@Serializable
data class IdentityStandard(
    val id: String,
    val uID: String = "",
    val title: String,
    val type: StandardType = StandardType.CUSTOM,
    val roleId: String,
    val xpReward: Int = 50,
    val isActive: Boolean = true
)
