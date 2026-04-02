package com.gcancino.levelingup.domain.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.models.player.PlayerData
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun getPlayerData(): Flow<Resource<PlayerData>>
    suspend fun syncMissingData(uid: String): Resource<Unit>
    suspend fun savePlayerData(playerData: PlayerData): Resource<Unit>
    fun getCurrentPlayer(): Flow<Resource<Player?>>
    suspend fun awardXP(uid: String, xp: Int): Resource<Int>
}
