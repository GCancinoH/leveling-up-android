package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gcancino.levelingup.data.local.database.entities.PlayerStreakEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PlayerStreakDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStreak(playerStreak: PlayerStreakEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePlayerStreak(playerStreak: PlayerStreakEntity)

    @Query("UPDATE player_streak SET currentStreak = :newStreak, lastStreakUpdate = :now, needSync = 1 WHERE uid = :uid")
    suspend fun updateStreak(uid: String, newStreak: Int, now: Date)

    @Query("SELECT * FROM player_streak WHERE uid = :playerId LIMIT 1")
    suspend fun getPlayerStreak(playerId: String): PlayerStreakEntity?

    @Query("SELECT * FROM player_streak WHERE uid = :playerId LIMIT 1")
    fun observePlayerStreak(playerId: String): Flow<PlayerStreakEntity?>

    // Resetear racha (penalización)
    @Query("UPDATE player_streak SET currentStreak = 0, lastStreakUpdate = :now, needSync = 1 WHERE uid = :uid")
    suspend fun resetStreak(uid: String, now: Long)

    // Incrementar racha (día de identidad cumplida)
    // Actualiza currentStreak, longestStreak si supera record, y fecha
    @Query("UPDATE player_streak SET currentStreak = :newStreak, longestStreak = CASE WHEN :newStreak > longestStreak THEN :newStreak ELSE longestStreak END, lastStreakUpdate = :now, needSync = 1 WHERE uid = :uid")
    suspend fun incrementStreak(uid: String, newStreak: Int, now: Long)

    // Consumir un token de protección
    @Query("UPDATE player_streak SET protectedDays = protectedDays - 1, needSync = 1 WHERE uid = :uid AND protectedDays > 0")
    suspend fun consumeProtectionToken(uid: String)

    // Añadir tokens de protección (recompensa por milestones)
    @Query("UPDATE player_streak SET protectedDays = protectedDays + :tokens, needSync = 1 WHERE uid = :uid")
    suspend fun addProtectionTokens(uid: String, tokens: Int)

    // Para sync nocturno
    @Query("SELECT * FROM player_streak WHERE needSync = 1")
    suspend fun getUnsynced(): List<PlayerStreakEntity>

    @Query("UPDATE player_streak SET needSync = 0, lastSync = :now WHERE uid IN (:uids)")
    suspend fun markAsSynced(uids: List<String>, now: Long)

    @Query("SELECT lastStreakUpdate FROM player_streak WHERE uid = :playerId LIMIT 1")
    fun getLastStreakUpdate(playerId: String): Flow<Long?>
}