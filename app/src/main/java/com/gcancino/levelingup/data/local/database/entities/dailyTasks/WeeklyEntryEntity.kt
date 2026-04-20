package com.gcancino.levelingup.data.local.database.entities.dailyTasks

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_entries",
    indices = [
        Index(value = ["uID"]),
        Index(value = ["year", "weekNumber"], unique = true)
    ]
)
data class WeeklyEntryEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val weekNumber: Int,
    val year: Int,
    val answers: String,        // Encrypted JSON
    val winHighlights: String,  // Encrypted JSON
    val alignmentScore: Float,
    val createdAt: Long,
    val isSynced: Boolean = false
)
