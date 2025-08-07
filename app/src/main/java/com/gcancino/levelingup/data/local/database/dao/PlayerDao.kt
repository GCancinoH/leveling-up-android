package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.entities.PlayerEntity
import com.gcancino.levelingup.data.local.database.entities.PlayerProgressEntity
import com.gcancino.levelingup.data.mappers.toEntity
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.models.player.Attributes
import com.gcancino.levelingup.domain.models.player.Improvement
import com.gcancino.levelingup.domain.models.player.Progress
import com.gcancino.levelingup.domain.models.player.Streak
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PlayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPLayer(player: PlayerEntity) : Long

    @Query("SELECT * FROM player WHERE uid = :uid")
    fun getPlayer(uid: String): PlayerEntity?

    @Query("SELECT improvements from player where uid = :uid")
    fun getPlayerImprovements(uid: String): List<Improvement>

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
    fun markForSync(uid: String)

    @Query("UPDATE player SET needs_sync = 0, last_sync = :lastSync WHERE uid = :uid")
    fun markAsSynced(uid: String, lastSync: Date = Date())
}