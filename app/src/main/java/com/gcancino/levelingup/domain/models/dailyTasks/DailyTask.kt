package com.gcancino.levelingup.domain.models.dailyTasks

import kotlinx.serialization.Serializable
import com.gcancino.levelingup.core.DateSerializer
import java.util.Date

@Serializable
data class DailyTask(
    val id: String = "",
    val uID: String = "",
    val objectiveId: String? = null, // Link to hierarchical objective
    val date: @Serializable(with = DateSerializer::class) Date = Date(),
    val title: String = "",
    val priority: TaskPriority = TaskPriority.LOW,   // 1 = highest
    val isCompleted: Boolean = false,
    val completedAt: @Serializable(with = DateSerializer::class) Date? = null,
    val xpReward: Int = XPScale.rewardForPriority(TaskPriority.LOW),
    val penaltyApplied: Boolean = false, // midnight worker sets this
    val isSynced: Boolean = false
)