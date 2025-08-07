package com.gcancino.levelingup.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val questID : String? = null,
    val types: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null, // QuestStatus enum as string
    val date: Long, // Date as timestamp
    val startedDate: Long? = null, // Date as timestamp
    val finishedDate: Long? = null, // Date as timestamp
    val rewards: String? = null, // JSON string of QuestRewards
    val details: String? = null, // JSON string of QuestDetails
    val streak: String? = null // JSON string of QuestStreak
)
