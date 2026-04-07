package com.gcancino.levelingup.data.local.database.entities.dailyTasks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "morning_entries")
data class MorningEntryEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val date: Date,
    val answers: String,   // JSON via TypeConverter (List<ReflectionAnswer>)
    val isSynced: Boolean = false
)