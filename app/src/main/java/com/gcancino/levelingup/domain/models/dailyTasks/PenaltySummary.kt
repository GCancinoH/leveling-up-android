package com.gcancino.levelingup.domain.models.dailyTasks

data class PenaltySummary(
    val xpLost: Int,
    val streakLost: Int,
    val incompleteTasks: Int
)
