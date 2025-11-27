package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.entities.QuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<QuestEntity>) : List<Long>

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Delete
    suspend fun deleteQuest(quest: QuestEntity)

    @Query("DELETE FROM quests")
    suspend fun deleteAllQuests()

    @Query("SELECT * FROM quests WHERE status = 'NOT_STARTED'")
    fun getNotStartedQuests() : Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE questID = :questId")
    suspend fun getQuestById(questId: String): QuestEntity?

    @Query("UPDATE quests SET status = 'IN_PROGRESS' WHERE id = :questId")
    suspend fun updateQuestStatusToInProgress(questId: String): Int

    @Query("UPDATE quests SET status = 'COMPLETED' WHERE id = :questId")
    suspend fun updateQuestStatusToCompleted(questId: String): Int

}