package com.gcancino.levelingup.data.local.database.dao.dailyTasks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.EveningEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EveningEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EveningEntryEntity)

    @Query("SELECT * FROM evening_entries WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay LIMIT 1")
    suspend fun getForDay(uID: String, startOfDay: Long, endOfDay: Long): EveningEntryEntity?

    @Query("SELECT COUNT(*) FROM evening_entries WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay")
    fun countForDay(uID: String, startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT * FROM evening_entries WHERE isSynced = 0")
    suspend fun getUnsynced(): List<EveningEntryEntity>

    @Query("UPDATE evening_entries SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}