package com.gcancino.levelingup.data.local.database.mappers

import com.gcancino.levelingup.data.local.database.entities.dailyTasks.DailyTaskEntity
import com.gcancino.levelingup.domain.models.dailyTasks.DailyTask
import com.google.gson.Gson

private val gson = Gson()

fun DailyTaskEntity.toDomain() = DailyTask(
    id = id,
    uID = uID,
    date = date,
    title = title,
    priority = priority,
    isCompleted = isCompleted,
    completedAt = completedAt,
    xpReward = xpReward,
    penaltyApplied = penaltyApplied,
    isSynced = isSynced
)

fun DailyTask.toEntity() = DailyTaskEntity(
    id = id,
    uID = uID,
    date = date,
    title = title,
    priority = priority,
    isCompleted = isCompleted,
    completedAt = completedAt,
    xpReward = xpReward,
    penaltyApplied = penaltyApplied,
    isSynced = isSynced
)