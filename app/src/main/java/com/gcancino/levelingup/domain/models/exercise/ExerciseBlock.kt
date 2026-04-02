package com.gcancino.levelingup.domain.models.exercise

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseBlock(
    val id: String = "",
    val sessionId: String = "",
    val type: BlockType = BlockType.Main,
    val sets: Int = 0,
    val restBetweenExercises: Int = 0,
    val restAfterBlock: Int = 0,
    val exercises: List<Exercise> = emptyList(),
    val order: Int = 0
)
