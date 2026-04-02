package com.gcancino.levelingup.domain.models.dailyTasks

import kotlinx.serialization.Serializable
import java.util.Date
import com.gcancino.levelingup.core.DateSerializer

@Serializable
data class EveningEntry(
    val id: String = "",
    val uID: String = "",
    val date: @Serializable(with = DateSerializer::class) Date = Date(),
    val answers: List<ReflectionAnswer> = emptyList(),
    val isSynced: Boolean = false
)