package com.gcancino.levelingup.data.local.database.dao.dailyTasks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.PenaltyEventEntity

@Dao
interface PenaltyEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: PenaltyEventEntity)

    @Query("SELECT * FROM penalty_events WHERE uID = :uID ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(uID: String): PenaltyEventEntity?

    @Query("SELECT * FROM penalty_events WHERE isSynced = 0")
    suspend fun getUnsynced(): List<PenaltyEventEntity>

    @Query("UPDATE penalty_events SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}