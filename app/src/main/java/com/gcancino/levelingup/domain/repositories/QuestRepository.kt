package com.gcancino.levelingup.domain.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.Quests
import kotlinx.coroutines.flow.Flow

interface QuestRepository {
    suspend fun syncQuestsFromFirestoreDos(): Resource<Unit>
    suspend fun syncQuestsFromFirestore(): Resource<Unit>
    fun getNotStartedQuests(): Flow<List<Quests>>
    suspend fun getQuestByQuestID(questID: String): Quests
}