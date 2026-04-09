package com.gcancino.levelingup.data.local.database.entities.identity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "generated_quests")
data class GeneratedQuestEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val weeklyReportId: String,
    val title: String,
    val description: String,
    val type: String,               // GeneratedQuestType.name
    val targetStandardIds: String,  // JSON → List<String>
    val goal: Int,
    val durationDays: Int,
    val currentProgress: Int = 0,
    val status: String = "ACTIVE",  // GeneratedQuestStatus.name
    val startDate: Date,
    val endDate: Date,
    val completedAt: Date? = null,
    val xpReward: Int = 150,
    val isSynced: Boolean = false
)