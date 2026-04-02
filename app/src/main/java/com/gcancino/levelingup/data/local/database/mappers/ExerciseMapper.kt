package com.gcancino.levelingup.data.local.database.mappers

import com.gcancino.levelingup.data.local.database.dao.ExerciseBlockWithExercises
import com.gcancino.levelingup.data.local.database.dao.ExerciseWithSets
import com.gcancino.levelingup.data.local.database.entities.*
import com.gcancino.levelingup.domain.models.exercise.*

fun TrainingSession.toEntity(microcycleId: String? = null): TrainingSessionEntity {
    return TrainingSessionEntity(
        id = id,
        macrocycleId = macrocycleId,
        mesocycleId = mesocycleId,
        microcycleId = microcycleId ?: this.microcycleId,
        date = date,
        name = name,
        completed = completed
    )
}

fun TrainingSessionEntity.toDomain(blocks: List<ExerciseBlock> = emptyList()): TrainingSession {
    return TrainingSession(
        id = id,
        date = date,
        name = name,
        completed = completed,
        blocks = blocks,
        microcycleId = microcycleId,
        macrocycleId = macrocycleId,
        mesocycleId = mesocycleId
    )
}

fun ExerciseBlock.toEntity(sessionId: String): ExerciseBlockEntity {
    return ExerciseBlockEntity(
        id = id,
        sessionId = sessionId,
        type = type,
        sets = sets,
        restBetweenExercises = restBetweenExercises,
        restAfterBlock = restAfterBlock,
        order = order
    )
}

fun ExerciseBlockWithExercises.toDomain(): ExerciseBlock {
    return ExerciseBlock(
        id = block.id,
        sessionId = block.sessionId,
        type = block.type,
        sets = block.sets,
        restBetweenExercises = block.restBetweenExercises,
        restAfterBlock = block.restAfterBlock,
        order = block.order,
        exercises = exercises.map { it.toDomain() }
    )
}

fun Exercise.toEntity(blockId: String): ExerciseEntity {
    return ExerciseEntity(
        id = id,
        blockId = blockId,
        name = name,
        role = exerciseRole,
        notes = notes,
        order = order
    )
}

fun ExerciseWithSets.toDomain(): Exercise {
    return Exercise(
        id = exercise.id,
        blockId = exercise.blockId,
        name = exercise.name,
        exerciseRole = exercise.role,
        notes = exercise.notes,
        order = exercise.order,
        sets = sets.map { it.toDomain() }
    )
}

fun ExerciseSet.toEntity(exerciseId: String, order: Int, setId: String = ""): ExerciseSetEntity {
    return ExerciseSetEntity(
        id = if (setId.isEmpty()) "${exerciseId}_$order" else setId,
        exerciseId = exerciseId,
        reps = reps,
        intensity = intensity,
        intensityType = intensityType,
        restSeconds = restSeconds,
        order = order
    )
}

fun ExerciseSetEntity.toDomain(): ExerciseSet {
    return ExerciseSet(
        id = id,
        reps = reps,
        intensity = intensity,
        intensityType = intensityType,
        restSeconds = restSeconds
    )
}
