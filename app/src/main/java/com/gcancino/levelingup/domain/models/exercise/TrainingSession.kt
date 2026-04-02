package com.gcancino.levelingup.domain.models.exercise

import com.gcancino.levelingup.core.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class TrainingSession(
    val id: String = "",
    val date: @Serializable(with = DateSerializer::class) Date = Date(),
    val name: String = "",
    val completed: Boolean = false,
    val blocks: List<ExerciseBlock> = emptyList(),
    val microcycleId: String? = null,
    val mesocycleId: String? = null,
    val macrocycleId: String? = null
)
