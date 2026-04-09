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
    val microHighlights: String = "[]",  // JSON List<String>
    val microConcerns: String   = "[]",  // JSON List<String>
    val processingLevel: String = "UNKNOWN",
    val alignment: String,               // NutritionAlignment.name
    val alignmentReason: String,
    val suggestion: String,
    val isSynced: Boolean = false
)