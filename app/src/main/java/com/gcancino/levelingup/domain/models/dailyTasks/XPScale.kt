package com.gcancino.levelingup.domain.models.dailyTasks

object XPScale {
    fun rewardForPriority(priority: TaskPriority): Int = when (priority) {
        TaskPriority.HIGH -> 10
        TaskPriority.INTERMEDIATE -> 5
        TaskPriority.LOW -> 2
    }

    fun penaltyForPriority(priority: TaskPriority): Int = rewardForPriority(priority)
}