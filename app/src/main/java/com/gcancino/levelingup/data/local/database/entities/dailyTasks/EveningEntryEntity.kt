package com.gcancino.levelingup.data.local.database.entities.dailyTasks

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "evening_entries")
data class EveningEntryEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val date: Date,
    val answers: String,   // JSON via TypeConverter
    val isSynced: Boolean = false
)
