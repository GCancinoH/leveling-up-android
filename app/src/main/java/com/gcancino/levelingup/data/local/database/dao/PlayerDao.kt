package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gcancino.levelingup.data.local.database.entities.PlayerEntity
import com.gcancino.levelingup.domain.models.player.Improvement
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PlayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPLayer(player: PlayerEntity) : Long

    @Query("SELECT * FROM player WHERE uid = :uid")
    suspend fun getPlayer(uid: String): PlayerEntity?

    @Query("SELECT improvements from player where uid = :uid")
    suspend fun getPlayerImprovements(uid: String): List<Improvement>

    @Query("SELECT needs_sync from player where uid = :uid")
    fun needsSync(uid: String): Flow<Boolean?>

    @Query("""
        UPDATE player SET displayName = :name, birthDate = :birthDate, gender = :gender
        WHERE uid = :uid
    """)
    suspend fun updateLocalPersonalData(uid: String, name: String, birthDate: Date, gender: String) : Int

    @Query("UPDATE player SET height = :height WHERE uid = :uid")
    suspend fun updateLocalHeight(uid: String, height: Double) : Int

    @Query("UPDATE player SET improvements = :improvements WHERE uid = :uid")
    suspend fun updateLocalImprovements(uid: String, improvements: List<Improvement>) : Int

    @Query("UPDATE player SET needs_sync = 1 WHERE uid = :uid")
    suspend fun markForSync(uid: String)

    @Query("UPDATE player SET needs_sync = 0, last_sync = :lastSync WHERE uid = :uid")
    suspend fun markAsSynced(uid: String, lastSync: Long)

    @Query("SELECT * FROM player WHERE needs_sync = 1 AND uid = :uid")
    suspend fun getUnsynced(uid: String): List<PlayerEntity>
}