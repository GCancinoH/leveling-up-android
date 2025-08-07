package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gcancino.levelingup.data.local.database.entities.PlayerAttributesEntity

@Dao
interface PlayerAttributesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerAttributes(playerAttributes: PlayerAttributesEntity): Long

    @Update
    suspend fun updatePlayerAttributes(playerAttributes: PlayerAttributesEntity): Int

    @Query("DELETE FROM player_attributes WHERE uid = :uid")
    suspend fun deletePlayerAttributes(uid: String): Int

    @Query("SELECT * FROM player_attributes WHERE uid = :uid LIMIT 1")
    suspend fun getPlayerAttributes(uid: String): PlayerAttributesEntity?

    @Query("SELECT needSync FROM player_attributes WHERE needSync=1 AND uid = :uid LIMIT 1")
    suspend fun getSyncStatus(uid: String): Boolean?

    @Query("UPDATE player_attributes SET needSync = :needSync, lastSync = :lastSync WHERE uid = :uid")
    suspend fun updateSyncStatus(uid: String, needSync: Boolean, lastSync: Long): Int

}