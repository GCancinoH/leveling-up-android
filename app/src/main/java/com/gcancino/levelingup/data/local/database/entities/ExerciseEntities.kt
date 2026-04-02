package com.gcancino.levelingup.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gcancino.levelingup.domain.models.exercise.BlockType
import com.gcancino.levelingup.domain.models.exercise.ExerciseRole
import com.gcancino.levelingup.domain.models.exercise.IntensityType
import java.util.Date

@Entity(tableName = "macrocycles")
data class MacrocycleEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val name: String,
    val description: String,
    val startDate: Date,
    val endDate: Date?
)

@Entity(
    tableName = "mesocycles",
    foreignKeys = [ForeignKey(
        entity = MacrocycleEntity::class,
        parentColumns = ["id"],
        childColumns = ["macrocycleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["macrocycleId"])]
)
data class MesocycleEntity(
    @PrimaryKey val id: String,
    val macrocycleId: String,
    val name: String,
    val description: String,
    val durationWeeks: Int,
    val startDate: Date,
    val endDate: Date
)

@Entity(
    tableName = "microcycles",
    foreignKeys = [ForeignKey(
        entity = MesocycleEntity::class,
        parentColumns = ["id"],
        childColumns = ["mesocycleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["mesocycleId"])]
)
data class MicrocycleEntity(
    @PrimaryKey val id: String,
    val mesocycleId: String,
    val name: String,
    val week: Int,
    val startDate: Date,
    val endDate: Date
)

/*@Entity(
    tableName = "training_sessions",
    foreignKeys = [ForeignKey(
        entity = MicrocycleEntity::class,
        parentColumns = ["id"],
        childColumns = ["microcycleId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index(value = ["microcycleId"])]
)
data class TrainingSessionEntity(
    @PrimaryKey val id: String,
    val microcycleId: String?,
    val date: Date,
    val name: String,
    val completed: Boolean
)*/
@Entity(
    tableName = "training_sessions",
    indices = [Index(value = ["microcycleId"])]
)
data class TrainingSessionEntity(
    @PrimaryKey val id: String,
    val completed: Boolean,
    val macrocycleId: String?,
    val mesocycleId: String?,
    val microcycleId: String?,
    val date: Date,
    val name: String
)


@Entity(
    tableName = "exercise_blocks",
    foreignKeys = [ForeignKey(
        entity = TrainingSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["sessionId"])]
)
data class ExerciseBlockEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val type: BlockType,
    val sets: Int,
    val restBetweenExercises: Int,
    val restAfterBlock: Int,
    val order: Int
)

@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = ExerciseBlockEntity::class,
        parentColumns = ["id"],
        childColumns = ["blockId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["blockId"])]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val blockId: String,
    val name: String,
    val role: ExerciseRole,
    val notes: String?,
    val order: Int
)

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [ForeignKey(
        entity = ExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["exerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["exerciseId"])]
)
data class ExerciseSetEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val reps: String,
    val intensity: Double,
    val intensityType: IntensityType,
    val restSeconds: Int,
    val order: Int,
    val durationSeconds: Int = 0
)

@Entity(
    tableName = "set_logs",
    indices   = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"])
    ]
    // No foreignKeys — parent tables are no longer populated
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val sessionId: String,
    val exerciseId: String,
    val setIndex: Int,
    val actualReps: Int?,
    val actualWeight: Double?,
    val isCompleted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
