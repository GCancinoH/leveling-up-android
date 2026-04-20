package com.gcancino.levelingup.domain.models.identity

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Objective(
    val id: String = UUID.randomUUID().toString(),
    val uID: String = "",
    val roleId: String = "",           // Linking to specific identity Role
    val parentId: String? = null,      // For hierarchy (e.g., Weekly links to Monthly)
    val title: String = "",
    val description: String = "",
    val horizon: TimeHorizon = TimeHorizon.WEEK,
    val status: ObjectiveStatus = ObjectiveStatus.ACTIVE,
    
    // Quantitative progress tracking
    val targetValue: Float? = null,    // e.g., 14.0 for body fat %
    val currentValue: Float = 0f,      // e.g., 16.0 current body fat %
    val unit: String? = null,          // e.g., "%", "kg", "tasks"
    
    // Temporal metadata
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    
    val isSynced: Boolean = false
) {
    /**
     * Percentage of progress toward the target.
     * Returns 0.0 to 1.0. If targetValue is null, treats it as a binary goal.
     */
    val progressFactor: Float
        get() = when {
            status == ObjectiveStatus.COMPLETED -> 1f
            targetValue == null || targetValue == 0f -> 0f
            else -> (currentValue / targetValue).coerceIn(0f, 1f)
        }
}
