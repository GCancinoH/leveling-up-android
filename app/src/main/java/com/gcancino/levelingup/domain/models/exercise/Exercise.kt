package com.gcancino.levelingup.domain.models.exercise

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String = "",
    val blockId: String = "",
    val name: String = "",
    val exerciseRole: ExerciseRole = ExerciseRole.ACCESSORY,
    val sets: List<ExerciseSet> = emptyList(),
    val volume: Double? = null,
    val notes: String? = null,
    val order: Int = 0
)
