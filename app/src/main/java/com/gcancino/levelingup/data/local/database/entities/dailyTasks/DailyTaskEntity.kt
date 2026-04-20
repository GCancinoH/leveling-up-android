package com.gcancino.levelingup.data.local.database.entities.dailyTasks

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gcancino.levelingup.domain.models.dailyTasks.TaskPriority
import java.util.Date

@Entity(
    tableName = "daily_tasks",
    indices = [
        Index(value = ["uID"]),
        Index(value = ["date"]),
        Index(value = ["objectiveId"])
    ]
)
data class DailyTaskEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val objectiveId: String? = null,
    val date: Date,
    val title: String,
    val priority: TaskPriority,
    val isCompleted: Boolean,
    val completedAt: Date?,
    val xpReward: Int,
    val penaltyApplied: Boolean = false,
    val isSynced: Boolean = false
)