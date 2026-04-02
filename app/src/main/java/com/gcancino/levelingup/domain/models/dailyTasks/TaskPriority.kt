package com.gcancino.levelingup.domain.models.dailyTasks

import androidx.compose.ui.graphics.Color

enum class TaskPriority(
    val xpReward: Int,
    val color: Color,
    val text: String
) {
    HIGH(10, Color(0xFFEF5350), "High"),
    INTERMEDIATE(5, Color(0xFFFFB300), "Intermediate"),
    LOW(2, Color(0xFF42A5F5), "Low")
}