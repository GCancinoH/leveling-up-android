package com.gcancino.levelingup.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "nutrition_entries")
data class NutritionEntryEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val date: Date,
    val foodIdentified: String,
    val photoUrl: String = "",
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatsG: Float,
    val fiberG: Float,
    val microHighlights: String = "[]",
    val microConcerns: String   = "[]",
    val processingLevel: String = "UNKNOWN",
    val alignment: String,
    val alignmentReason: String,
    val suggestion: String,
    val alignmentScore: Float = 0f,
    val action: String = "",
    val isSynced: Boolean = false
)