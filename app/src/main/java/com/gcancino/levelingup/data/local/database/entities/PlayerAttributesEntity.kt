package com.gcancino.levelingup.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "player_attributes")
data class PlayerAttributesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String = "",
    val strength: Int? = null,
    val endurance: Int? = null,
    val intelligence: Int? = null,
    val mobility: Int? = null,
    val health: Int? = null,
    val finance: Int? = null,
    val needSync : Boolean? = false,
    val lastSync : Date? = null
)
