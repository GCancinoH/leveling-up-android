package com.gcancino.levelingup.data.local.database.entities.bodyData

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val date: Date,
    // Core
    val neck: Double,
    val shoulders: Double,
    val chest: Double,
    val waist: Double,
    val umbilical: Double,
    val hip: Double,
    // Arms
    val bicepLeftRelaxed: Double,
    val bicepLeftFlexed: Double,
    val bicepRightRelaxed: Double,
    val bicepRightFlexed: Double,
    val forearmLeft: Double,
    val forearmRight: Double,
    // Legs
    val thighLeft: Double,
    val thighRight: Double,
    val calfLeft: Double,
    val calfRight: Double,
    val initialData: Boolean,
    val unitSystem: String,         // stored as "METRIC" or "IMPERIAL"
    val isSynced: Boolean = false
)