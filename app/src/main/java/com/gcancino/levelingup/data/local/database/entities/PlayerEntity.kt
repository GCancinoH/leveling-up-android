package com.gcancino.levelingup.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gcancino.levelingup.domain.models.player.Genders
import com.gcancino.levelingup.domain.models.player.Improvement
import java.util.Date

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val uid: String,
    val displayName: String? = null,
    val email: String? = null,
    val photoURL: String? = null,
    val birthDate: Date? = null,
    val age: Int? = null,
    val height: Double? = null,
    val gender: Genders? = null,
    val improvements: List<Improvement>? = emptyList(),
    @ColumnInfo(name = "last_sync")
    val lastSync: Date = Date(),
    @ColumnInfo(name = "needs_sync")
    val needsSync: Boolean = false
)

