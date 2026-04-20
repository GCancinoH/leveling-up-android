package com.gcancino.levelingup.domain.models.dailyTasks

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WeeklyEntry(
    val id: String = UUID.randomUUID().toString(),
    val uID: String = "",
    val weekNumber: Int,
    val year: Int,
    
    // Neuro-Alignment questions
    val answers: List<ReflectionAnswer> = emptyList(),
    
    // Success patterns
    val winHighlights: List<String> = emptyList(),
    
    // Quantitative Alignment (0.0 to 1.0)
    // Derived from the percentage of completed standards/objectives for the week.
    val alignmentScore: Float = 0f,
    
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
