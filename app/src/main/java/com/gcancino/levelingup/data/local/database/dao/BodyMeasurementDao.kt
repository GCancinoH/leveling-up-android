package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gcancino.levelingup.data.local.database.entities.bodyData.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BodyMeasurementEntity)

    @Query("SELECT * FROM body_measurements WHERE uID = :uID ORDER BY date DESC")
    fun getAll(uID: String): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements WHERE uID = :uID AND initialData = 1 LIMIT 1")
    suspend fun getInitialData(uID: String): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurements WHERE isSynced = 0")
    suspend fun getUnsynced(): List<BodyMeasurementEntity>

    @Query("UPDATE body_measurements SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM body_measurements WHERE uID = :uID AND initialData = 1")
    suspend fun countInitialData(uID: String): Int
}
