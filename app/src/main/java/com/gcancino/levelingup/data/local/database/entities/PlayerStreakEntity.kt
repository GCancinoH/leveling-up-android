package com.gcancino.levelingup.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_streak")
data class PlayerStreakEntity(
    @PrimaryKey(autoGenerate = false) val id: Long = 0L,
    val uid: String = "",
    val currentStreak: Int? = null,
    val longestStreak: Int? = null,
    val lastStreakUpdate: Long? = null,
    val needSync: Boolean? = false,
    val lastSync: Long? = null
)
