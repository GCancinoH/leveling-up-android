package com.gcancino.levelingup.data.local.database.dao

import androidx.room.*
import com.gcancino.levelingup.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    // ─── Inserts ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacrocycle(macrocycles: List<MacrocycleEntity>)  // ← was single entity, now list

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMesocycles(mesocycles: List<MesocycleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMicrocycles(microcycles: List<MicrocycleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingSessions(sessions: List<TrainingSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseBlocks(blocks: List<ExerciseBlockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSets(sets: List<ExerciseSetEntity>)

    // ─── Session Queries ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM training_sessions WHERE date >= :startOfDay AND date < :endOfDay")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<TrainingSessionEntity>>

    @Query("SELECT * FROM training_sessions WHERE date >= :startOfDay AND date < :endOfDay")
    suspend fun getSessionsForDayOnce(startOfDay: Long, endOfDay: Long): List<TrainingSessionEntity>

    @Query("SELECT * FROM training_sessions WHERE date >= :weekStart AND date < :weekEnd")
    suspend fun getSessionsForWeekOnce(weekStart: Long, weekEnd: Long): List<TrainingSessionEntity>

    /**
     * Seed guard: returns 0 if the week has never been synced.
     * Used by syncWeek() to decide whether to hit Firestore.
     */
    @Query("SELECT COUNT(*) FROM training_sessions WHERE date >= :weekStart AND date < :weekEnd")
    suspend fun countSessionsForWeek(weekStart: Long, weekEnd: Long): Int

    // ─── Block Queries ────────────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM exercise_blocks WHERE sessionId = :sessionId ORDER BY `order` ASC")
    fun getBlocksForSession(sessionId: String): Flow<List<ExerciseBlockWithExercises>>

    @Transaction
    @Query("SELECT * FROM exercise_blocks WHERE sessionId = :sessionId ORDER BY `order` ASC")
    suspend fun getBlocksForSessionOnce(sessionId: String): List<ExerciseBlockWithExercises>

    // ─── Set Logs ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(log: SetLogEntity)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId")
    fun getLogsForSession(sessionId: String): Flow<List<SetLogEntity>>

    @Query("DELETE FROM set_logs WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND setIndex = :setIndex")
    suspend fun deleteSetLog(sessionId: String, exerciseId: String, setIndex: Int)
}

// ─── Relations ────────────────────────────────────────────────────────────────────

data class ExerciseBlockWithExercises(
    @Embedded val block: ExerciseBlockEntity,
    @Relation(
        entity = ExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "blockId"
    )
    val exercises: List<ExerciseWithSets>
)

data class ExerciseWithSets(
    @Embedded val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val sets: List<ExerciseSetEntity>
)