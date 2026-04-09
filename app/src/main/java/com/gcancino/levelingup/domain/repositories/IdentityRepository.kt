package com.gcancino.levelingup.domain.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.identity.DailyStandardEntry
import com.gcancino.levelingup.domain.models.identity.IdentityProfile
import com.gcancino.levelingup.domain.models.identity.IdentityScore
import kotlinx.coroutines.flow.Flow

interface IdentityRepository {
    suspend fun saveIdentityProfile(profile: IdentityProfile): Resource<Unit>
    fun observeIdentityProfile(uID: String): Flow<IdentityProfile?>
    suspend fun hasIdentityProfile(uID: String): Boolean
    suspend fun generateTodayEntries(uID: String): Resource<Unit>
    fun observeTodayScore(uID: String): Flow<IdentityScore>
    fun observeTodayEntries(uID: String): Flow<List<DailyStandardEntry>>
    fun observePendingEntries(uID: String): Flow<List<DailyStandardEntry>>
    suspend fun completeStandard(entryId: String, uID: String): Resource<Int>
    suspend fun autoValidateTraining(uID: String): Resource<Unit>
    suspend fun autoValidateNutrition(uID: String): Resource<Unit>
    suspend fun applyMidnightPenalty(uID: String): Resource<Unit>
    suspend fun syncUnsynced(): Resource<Unit>
}