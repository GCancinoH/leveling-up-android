package com.gcancino.levelingup.data.local.database.dao.dailyTasks

import androidx.room.*
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.WeeklyEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeeklyEntryEntity)

    @Query("SELECT * FROM weekly_entries WHERE uID = :uID AND year = :year AND weekNumber = :weekNumber")
    suspend fun getByWeek(uID: String, year: Int, weekNumber: Int): WeeklyEntryEntity?

    @Query("SELECT * FROM weekly_entries WHERE uID = :uID AND year = :year AND weekNumber = :weekNumber")
    fun observeByWeek(uID: String, year: Int, weekNumber: Int): Flow<WeeklyEntryEntity?>

    @Query("SELECT * FROM weekly_entries WHERE uID = :uID ORDER BY year DESC, weekNumber DESC")
    fun observeAll(uID: String): Flow<List<WeeklyEntryEntity>>

    @Query("SELECT * FROM weekly_entries WHERE isSynced = 0")
    suspend fun getUnsynced(): List<WeeklyEntryEntity>

    @Query("UPDATE weekly_entries SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
