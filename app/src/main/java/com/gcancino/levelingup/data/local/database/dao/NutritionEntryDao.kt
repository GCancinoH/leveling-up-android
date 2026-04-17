package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gcancino.levelingup.data.local.database.entities.NutritionEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionEntryDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(entity: NutritionEntryEntity): Unit

    // Today's entries — for the daily nutrition dashboard
    @Query("""
        SELECT * FROM nutrition_entries 
        WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay 
        ORDER BY date DESC
    """)
    fun observeForDay(uID: String, startOfDay: Long, endOfDay: Long): Flow<List<NutritionEntryEntity>>

    // Count aligned meals today — for NUTRITION standard auto-validation
    @Query("""
        SELECT COUNT(*) FROM nutrition_entries 
        WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay 
        AND alignment = 'ALIGNED'
    """)
    fun countAlignedForDay(uID: String, startOfDay: Long, endOfDay: Long): Flow<Int>

    // Recent history — for Identity Wall nutrition section
    @Query("""
        SELECT * FROM nutrition_entries 
        WHERE uID = :uID ORDER BY date DESC LIMIT 30
    """)
    fun observeRecent(uID: String): Flow<List<NutritionEntryEntity>>

    @Query("SELECT * FROM nutrition_entries WHERE isSynced = 0")
    suspend fun getUnsynced(): List<NutritionEntryEntity>

    @Query("UPDATE nutrition_entries SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}