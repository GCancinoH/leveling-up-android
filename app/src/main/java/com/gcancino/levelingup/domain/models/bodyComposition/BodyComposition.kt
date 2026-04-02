package com.gcancino.levelingup.domain.models.bodyComposition

import com.gcancino.levelingup.core.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class BodyComposition(
    val id: String = "",
    val uID: String = "",
    val date: @Serializable(with = DateSerializer::class) Date = Date(),
    val weight: Double = 0.0,
    val bmi: Double = 0.0,
    val bodyFatPercentage: Double = 0.0,
    val muscleMassPercentage: Double = 0.0,
    val visceralFat: Double = 0.0,
    val bodyAge: Int = 0,
    val initialData: Boolean = false,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val photos: List<String> = emptyList(),
    val isSynced: Boolean = false
)
