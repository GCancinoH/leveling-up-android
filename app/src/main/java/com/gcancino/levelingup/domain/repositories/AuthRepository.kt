package com.gcancino.levelingup.domain.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.models.player.Improvement
import com.gcancino.levelingup.domain.models.player.PlayerData
import com.gcancino.levelingup.utils.Partial
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface AuthRepository {
    suspend fun signInWithEmailAndPassword(email: String, password: String): Resource<Player>
    fun signUpWithEmailAndPassword(email: String, password: String): Flow<Resource<Unit>>
    fun savePersonalInfoData(name: String, birthdate: Date, gender: String) : Flow<Resource<PlayerData>>
    fun savePhysicalAttributesData(height: String, weight: String, bmi: String) : Flow<Resource<Unit>>
    fun saveImprovementData(improvement: List<Improvement>): Flow<Resource<Unit>>
    suspend fun forgotPassword(email: String): Resource<Boolean>
    suspend fun signOut(): Resource<Boolean>
    fun getCurrentPlayer(): Flow<Resource<Player?>>
    fun getAuthState(): Flow<Resource<Player?>>
}