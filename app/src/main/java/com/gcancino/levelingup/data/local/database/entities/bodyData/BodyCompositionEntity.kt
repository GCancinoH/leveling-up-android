package com.gcancino.levelingup.data.local.database.entities.bodyData

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "body_composition")
data class BodyCompositionEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val date: Date,
    val weight: Double,
    val bmi: Double,
    val bodyFatPercentage: Double,
    val muscleMassPercentage: Double,
    val visceralFat: Double,
    val bodyAge: Int,
    val initialData: Boolean,
    val photos: List<String> = emptyList(),
    val unitSystem: String,
    val isSynced: Boolean = false
)
