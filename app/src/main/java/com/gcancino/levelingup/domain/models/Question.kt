package com.gcancino.levelingup.domain.models

data class Question(
    val id: String,
    val text: String,
    val hint: String    = "",
    val isAnchor: Boolean = false
)