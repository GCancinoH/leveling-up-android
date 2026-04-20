package com.gcancino.levelingup.data.local.database.dao.identity

import androidx.room.*
import com.gcancino.levelingup.data.local.database.entities.identity.ObjectiveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectiveDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(objective: ObjectiveEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(objectives: List<ObjectiveEntity>)

    @Query("SELECT * FROM objectives WHERE id = :id")
    suspend fun getById(id: String): ObjectiveEntity?

    @Query("SELECT * FROM objectives WHERE uID = :uID ORDER BY createdAt DESC")
    fun observeAll(uID: String): Flow<List<ObjectiveEntity>>

    @Query("SELECT * FROM objectives WHERE uID = :uID AND horizon = :horizon ORDER BY createdAt DESC")
    fun observeByHorizon(uID: String, horizon: String): Flow<List<ObjectiveEntity>>

    @Query("SELECT * FROM objectives WHERE parentId = :parentId")
    suspend fun getChildren(parentId: String): List<ObjectiveEntity>

    @Query("SELECT * FROM objectives WHERE roleId = :roleId")
    fun observeByRole(roleId: String): Flow<List<ObjectiveEntity>>

    @Update
    suspend fun update(objective: ObjectiveEntity)

    @Query("UPDATE objectives SET currentValue = :newValue, isSynced = 0 WHERE id = :id")
    suspend fun updateProgress(id: String, newValue: Float)

    @Query("SELECT * FROM objectives WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ObjectiveEntity>

    @Query("UPDATE objectives SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
    
    @Delete
    suspend fun delete(objective: ObjectiveEntity)
}
