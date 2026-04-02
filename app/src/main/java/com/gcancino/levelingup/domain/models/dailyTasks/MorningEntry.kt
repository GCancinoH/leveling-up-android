package com.gcancino.levelingup.domain.models.dailyTasks

import kotlinx.serialization.Serializable
import com.gcancino.levelingup.core.DateSerializer
import java.util.Date

@Serializable
data class MorningEntry(
    val id: String = "",
    val uID: String = "",
    val date: @Serializable(with = DateSerializer::class) Date = Date(),
    val answers: List<ReflectionAnswer> = emptyList(),
    val isSynced: Boolean = false
)