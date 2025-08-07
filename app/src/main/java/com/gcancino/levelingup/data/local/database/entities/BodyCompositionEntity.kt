package com.gcancino.levelingup.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "body_composition")
data class BodyCompositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uid: String,
    val weight: Double? = null,
    val bmi: Double? = null,
    val bodyFat: Double? = null,
    val muscleMass: Double? = null,
    val bodyAge: Int? = null,
    val visceralFat: Int? = null,
    val photos: List<String>? = null,
    val date: Date? = null,
    val isInitial: Boolean = false
)
