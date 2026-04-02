package com.gcancino.levelingup.domain.models.exercise

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseSet(
    val id: String = "",
    val reps: String = "",
    val durationSeconds: Int = 0,
    val intensity: Double = 0.0,
    val intensityType: IntensityType = IntensityType.RPE,
    val restSeconds: Int = 0,
    val isDropSet: Boolean = false
)
