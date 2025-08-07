package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gcancino.levelingup.data.local.database.entities.PlayerStreakEntity
import kotlinx.coroutines.flow.Flow
import java.sql.Time
import java.sql.Timestamp
import java.util.Date

@Dao
interface PlayerStreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStreak(playerStreak: PlayerStreakEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePlayerStreak(playerStreak: PlayerStreakEntity)

    @Query("SELECT * FROM player_streak WHERE uid = :playerId LIMIT 1")
    suspend fun getPlayerStreak(playerId: String): PlayerStreakEntity?

    @Query("UPDATE player_streak SET currentStreak = :currentStreak, lastStreakUpdate = :newStreakUpdate WHERE uid = :playerId")
    suspend fun updateStreak(playerId: String, currentStreak: Int, newStreakUpdate: Date)

    @Query("SELECT lastStreakUpdate FROM player_streak WHERE uid = :playerId LIMIT 1")
    fun getLastStreakUpdate(playerId: String): Flow<Long?>


}