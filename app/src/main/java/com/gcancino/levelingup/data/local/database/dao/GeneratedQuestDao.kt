package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gcancino.levelingup.data.local.database.entities.identity.GeneratedQuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedQuestDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(quest: GeneratedQuestEntity)

    // Active quests — shown in dashboard and tracked daily
    @Query("SELECT * FROM generated_quests WHERE uID = :uID AND status = 'ACTIVE' ORDER BY startDate DESC")
    fun observeActive(uID: String): Flow<List<GeneratedQuestEntity>>

    // All quests — for Identity Wall history
    @Query("SELECT * FROM generated_quests WHERE uID = :uID ORDER BY startDate DESC LIMIT 20")
    fun observeAll(uID: String): Flow<List<GeneratedQuestEntity>>

    // Current active quest — for DailyResetManager to update progress
    @Query("SELECT * FROM generated_quests WHERE uID = :uID AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActive(uID: String): GeneratedQuestEntity?

    // Update progress after a day of standard completion
    @Query("UPDATE generated_quests SET currentProgress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int)

    @Query("UPDATE generated_quests SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, completedAt: Long?)

    @Query("SELECT * FROM generated_quests WHERE isSynced = 0")
    suspend fun getUnsynced(): List<GeneratedQuestEntity>

    @Query("UPDATE generated_quests SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}