package com.gcancino.levelingup.domain.models.dailyTasks

import kotlinx.serialization.Serializable
import java.util.Date
import com.gcancino.levelingup.core.DateSerializer

@Serializable
data class PenaltyEvent(
    val id: String = "",
    val uID: String = "",
    val date: @Serializable(with = DateSerializer::class) Date = Date(),
    val xpLost: Int = 0,
    val streakLost: Int = 0,    // streak value before reset
    val incompleteTasks: List<String> = emptyList(), // task IDs
    val isSynced: Boolean = false
    // Future: tokenAmount: Double = 0.0
)
