package com.gcancino.levelingup.domain.models.bodyComposition

import kotlinx.serialization.Serializable
import java.util.Date
import com.gcancino.levelingup.core.DateSerializer

@Serializable
data class BodyMeasurement(
    val id: String = "",
    val uID: String = "",
    val date: @Serializable(with = DateSerializer::class) Date  =Date(),
    // Core
    val neck: Double = 0.0,
    val shoulders: Double = 0.0,
    val chest: Double = 0.0,
    val waist: Double = 0.0,
    val umbilical: Double = 0.0,
    val hip: Double = 0.0,
    // Arms
    val bicepLeftRelaxed: Double = 0.0,
    val bicepLeftFlexed: Double = 0.0,
    val bicepRightRelaxed: Double = 0.0,
    val bicepRightFlexed: Double = 0.0,
    val forearmLeft: Double = 0.0,
    val forearmRight: Double = 0.0,
    // Legs
    val thighLeft: Double = 0.0,
    val thighRight: Double = 0.0,
    val calfLeft: Double = 0.0,
    val calfRight: Double = 0.0,
    val initialData: Boolean = false,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val isSynced: Boolean = false
)
