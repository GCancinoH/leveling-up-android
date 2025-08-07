package com.gcancino.levelingup.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gcancino.levelingup.domain.models.player.CategoryType
import java.util.Date

@Entity(tableName = "player_progress")
data class PlayerProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String = "",
    val coins: Int? = null,
    val exp: Int? = null,
    val level: Int? = null,
    val currentCategory: String? = null,
    val lastLevelUpdate: Date? = null,
    val lastCategoryUpdate: Date? = null,
    val needSync: Boolean = false,
    val lastSync: Date? = null
)
