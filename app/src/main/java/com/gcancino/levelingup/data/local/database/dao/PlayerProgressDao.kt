package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gcancino.levelingup.data.local.database.entities.PlayerProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerProgress(progress: PlayerProgressEntity) : Long

    @Update
    suspend fun updatePlayerProgress(progress: PlayerProgressEntity): Int

    @Query("DELETE FROM player_progress WHERE uid = :uid")
    suspend fun deletePlayerProgress(uid: String): Int

    @Query("SELECT exp FROM player_progress WHERE uid = :uid")
    fun getCurrentXP(uid: String): Flow<Int?>

    @Query("SELECT level FROM player_progress WHERE uid = :uid")
    fun getCurrentLevel(uid: String): Flow<Int?>

    @Query("SELECT * FROM player_progress WHERE uid = :uid LIMIT 1")
    suspend fun getPlayerProgress(uid: String): PlayerProgressEntity?

    @Query("UPDATE player_progress SET needSync = :needSync, lastSync = :lastSync WHERE uid = :uid")
    suspend fun updateSyncStatus(uid: String, needSync: Boolean, lastSync: Long): Int

    /* Updates */
    @Query("UPDATE player_progress SET exp = :xp WHERE uid = :uid")
    suspend fun updateXP(uid: String, xp: Int): Int

    @Query("UPDATE player_progress SET level = :level WHERE uid = :uid")
    suspend fun updateLevel(uid: String, level: Int): Int

    @Query("UPDATE player_progress SET availablePoints = :points, level = :level, exp = :xp WHERE uid = :uid")
    suspend fun updateProgress(uid: String, points: Int, level: Int, xp: Int): Int





}