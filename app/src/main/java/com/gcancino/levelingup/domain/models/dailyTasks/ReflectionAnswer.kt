package com.gcancino.levelingup.domain.models.dailyTasks

import kotlinx.serialization.Serializable

@Serializable
data class ReflectionAnswer(
    val questionId: String,
    val questionText: String,
    val answer: String
)