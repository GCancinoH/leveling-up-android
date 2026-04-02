package com.gcancino.levelingup.data.local.database.dao.dailyTasks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.MorningEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MorningEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MorningEntryEntity)

    @Query("SELECT * FROM morning_entries WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay LIMIT 1")
    suspend fun getForDay(uID: String, startOfDay: Long, endOfDay: Long): MorningEntryEntity?

    @Query("SELECT COUNT(*) FROM morning_entries WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay")
    fun countForDay(uID: String, startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT * FROM morning_entries WHERE isSynced = 0")
    suspend fun getUnsynced(): List<MorningEntryEntity>

    @Query("UPDATE morning_entries SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}