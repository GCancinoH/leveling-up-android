package com.gcancino.levelingup.domain.models.event

import com.gcancino.levelingup.core.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class SystemEventLog(
    val id: String = "",
    val uID: String = "",
    val type: EventType,
    val sourceEvent: String,        // Ej: "NutritionAnalyzed", "TaskCompleted"
    val impact: String,             // Ej: "+50 XP", "-10 XP, streak reset", "Quest progressed"
    val details: Map<String, String> = emptyMap(),  // Datos adicionales
    @Serializable(with = DateSerializer::class)
    val timestamp: Date = Date(),
    val isSynced: Boolean = false
) {
    enum class EventType {
        NUTRITION,
        TASK,
        TRAINING,
        MORNING_FLOW,
        EVENING_FLOW,
        QUEST,
        STREAK,
        PENALTY,
        LEVEL_UP,
        ROLE_PROGRESS
    }
}