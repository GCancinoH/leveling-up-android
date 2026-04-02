package com.gcancino.levelingup.data.local.database.dao.dailyTasks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.DailyTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<DailyTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DailyTaskEntity)

    @Query("SELECT * FROM daily_tasks WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay ORDER BY priority ASC")
    fun getTasksForDay(uID: String, startOfDay: Long, endOfDay: Long): Flow<List<DailyTaskEntity>>

    @Query("SELECT * FROM daily_tasks WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay ORDER BY priority ASC")
    suspend fun getTasksForDayOnce(uID: String, startOfDay: Long, endOfDay: Long): List<DailyTaskEntity>

    @Query("SELECT * FROM daily_tasks WHERE uID = :uID AND isCompleted = 0 AND date >= :startOfDay AND date < :endOfDay ORDER BY priority ASC")
    fun getPendingTasksForDay(uID: String, startOfDay: Long, endOfDay: Long): Flow<List<DailyTaskEntity>>

    @Query("UPDATE daily_tasks SET isCompleted = 1, completedAt = :completedAt, isSynced = 0 WHERE id = :taskId")
    suspend fun markCompleted(taskId: String, completedAt: Long)

    @Query("UPDATE daily_tasks SET penaltyApplied = 1 WHERE id = :taskId")
    suspend fun markPenaltyApplied(taskId: String)

    @Query("SELECT * FROM daily_tasks WHERE isSynced = 0")
    suspend fun getUnsynced(): List<DailyTaskEntity>

    @Query("UPDATE daily_tasks SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM daily_tasks WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay")
    suspend fun countTasksForDay(uID: String, startOfDay: Long, endOfDay: Long): Int
}