package com.gcancino.levelingup.domain.models.identity

import kotlinx.serialization.Serializable

@Serializable
data class Role(
    val id: String,
    val name: String,
    val icon: String = "⚡",
    val color: String = "7986CB"
)