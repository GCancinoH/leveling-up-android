package com.gcancino.levelingup.data.local.database.entities.dailyTasks

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "penalty_events")
data class PenaltyEventEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val date: Date,
    val xpLost: Int,
    val streakLost: Int,
    val incompleteTasks: String,  // JSON list of task IDs
    val isSynced: Boolean = false
)
