package com.gcancino.levelingup.data.local.database.dao

import androidx.compose.runtime.ReusableComposition
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.entities.BodyCompositionEntity
import com.gcancino.levelingup.data.mappers.toEntity
import com.gcancino.levelingup.domain.models.BodyComposition
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyCompositionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyComposition(bodyComposition: BodyCompositionEntity): Long

    @Update
    suspend fun updateBodyComposition(bodyComposition: BodyCompositionEntity): Int

    @Delete
    suspend fun deleteBodyComposition(bodyComposition: BodyCompositionEntity): Int

    @Query("SELECT * FROM body_composition WHERE uid = :uid ORDER BY date DESC")
    fun getBodyComposition(uid: String): Flow<BodyCompositionEntity?>

    @Query("SELECT * FROM body_composition WHERE uid = :uid AND isInitial = 1 LIMIT 1")
    suspend fun getInitialBodyComposition(uid: String): BodyCompositionEntity?

    /*
     * Queries for updating existing data
     */
    @Query("UPDATE body_composition SET weight = :weight WHERE uid = :uid")
    suspend fun updateWeight(uid: String, weight: Double): Int

    @Query("UPDATE body_composition SET bmi = :bmi WHERE uid = :uid")
    suspend fun updateBMI(uid: String, bmi: Double): Int

    @Query("UPDATE body_composition SET bodyFat = :bodyFat WHERE uid = :uid")
    suspend fun updateBodyFat(uid: String, bodyFat: Double): Int

    @Query("UPDATE body_composition SET muscleMass = :muscleMass WHERE uid = :uid")
    suspend fun updateMuscleMass(uid: String, muscleMass: Double): Int

    @Query("UPDATE body_composition SET bodyAge = :bodyAge WHERE uid = :uid")
    suspend fun updateBodyAge(uid: String, bodyAge: Int): Int

    @Query("UPDATE body_composition SET visceralFat = :visceralFat WHERE uid = :uid")
    suspend fun updateVisceralFat(uid: String, visceralFat: Int): Int

    /*
     * Additional queries for initial data
     */
    @Query("UPDATE body_composition SET weight = :weight WHERE uid = :uid AND isInitial = 1")
    suspend fun updateInitialWeight(uid: String, weight: Double): Int

    @Query("UPDATE body_composition SET bmi = :bmi WHERE uid = :uid AND isInitial = 1")
    suspend fun updateInitialBMI(uid: String, bmi: Double): Int

    @Query("UPDATE body_composition SET bodyFat = :bodyFat WHERE uid = :uid AND isInitial = 1")
    suspend fun updateInitialBodyFat(uid: String, bodyFat: Double): Int

    @Query("UPDATE body_composition SET muscleMass = :muscleMass WHERE uid = :uid AND isInitial = 1")
    suspend fun updateInitialMuscleMass(uid: String, muscleMass: Double): Int

    @Query("UPDATE body_composition SET bodyAge = :bodyAge WHERE uid = :uid AND isInitial = 1")
    suspend fun updateInitialBodyAge(uid: String, bodyAge: Int): Int

    @Query("UPDATE body_composition SET visceralFat = :visceralFat WHERE uid = :uid AND isInitial = 1")
    suspend fun updateInitialVisceralFat(uid: String, visceralFat: Int): Int

    @Query("""
        UPDATE body_composition SET bodyFat = :bodyFat, muscleMass = :muscleMass,
        visceralFat = :visceralFat, bodyAge = :bodyAge WHERE uid = :uid AND
        isInitial = 1
    """)
    suspend fun updateInitialBodyComposition(uid: String, bodyFat: Double, muscleMass: Double, visceralFat: Int, bodyAge: Int): Int

    @Query("UPDATE body_composition SET photos = :photos WHERE uid = :uid AND isInitial = 1")
    suspend fun updateInitialPhotos(uid: String, photos: List<String>): Int


}