package com.gcancino.levelingup.data.local.database.entities.identity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "objectives",
    indices = [
        Index(value = ["uID"]),
        Index(value = ["roleId"]),
        Index(value = ["parentId"]),
        Index(value = ["horizon"])
    ]
)
data class ObjectiveEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val roleId: String,
    val parentId: String?,
    val title: String,
    val description: String, // Will be encrypted
    val horizon: String,     // Enum name
    val status: String,      // Enum name
    val targetValue: Float?,
    val currentValue: Float,
    val unit: String?,
    val startDate: Long,
    val endDate: Long?,
    val createdAt: Long,
    val completedAt: Long?,
    val isSynced: Boolean = false
)
