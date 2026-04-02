package com.gcancino.levelingup.ui.theme

import androidx.compose.ui.graphics.Color
import com.gcancino.levelingup.domain.models.dailyTasks.TaskPriority

object SystemColors {
    val BackgroundColor = Color(0xFF050505)
    val CardBackground = Color(0xFF111120)
    val PrimaryColor = Color(0xFF5B00E6)
    val GlowBlue = Color(0xFF4285F4)
    val GlowPurple = Color(0xFFA64BF4)
    val TextColor = Color(0xFFE6E6E6)
    val ErrorColor = Color(0xFFFF4545)
    val BorderColor = GlowBlue.copy(alpha = 0.2f)
}

fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.HIGH -> Color(0xFFE53935)
    TaskPriority.INTERMEDIATE -> Color(0xFFFF6F00)
    TaskPriority.LOW -> Color(0xFF1E88E5)
}
