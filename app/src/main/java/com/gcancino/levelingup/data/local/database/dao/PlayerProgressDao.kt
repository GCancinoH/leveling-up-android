package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gcancino.levelingup.data.local.database.entities.PlayerProgressEntity

@Dao
interface PlayerProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerProgress(progress: PlayerProgressEntity) : Long

    @Update
    suspend fun updatePlayerProgress(progress: PlayerProgressEntity): Int

    @Query("DELETE FROM player_progress WHERE uid = :uid")
    suspend fun deletePlayerProgress(uid: String): Int

    @Query("SELECT * FROM player_progress WHERE uid = :uid LIMIT 1")
    suspend fun getPlayerProgress(uid: String): PlayerProgressEntity

    @Query("UPDATE player_progress SET needSync = :needSync, lastSync = :lastSync WHERE uid = :uid")
    suspend fun updateSyncStatus(uid: String, needSync: Boolean, lastSync: Long): Int
}