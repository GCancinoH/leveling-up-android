package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gcancino.levelingup.data.local.database.entities.DailyStandardEntryEntity
import com.gcancino.levelingup.data.local.database.entities.IdentityProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Identity Profile
 */
@Dao
interface IdentityProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: IdentityProfileEntity)

    @Query("SELECT * FROM identity_profile WHERE uID = :uID LIMIT 1")
    fun observe(uID: String): Flow<IdentityProfileEntity?>

    @Query("SELECT * FROM identity_profile WHERE uID = :uID LIMIT 1")
    suspend fun get(uID: String): IdentityProfileEntity?

    @Query("SELECT COUNT(*) FROM identity_profile WHERE uID = :uID")
    suspend fun count(uID: String): Int

    @Query("SELECT * FROM identity_profile WHERE isSynced = 0")
    suspend fun getUnsynced(): List<IdentityProfileEntity>

    @Query("UPDATE identity_profile SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}

/**
 * Daily Standard Entry
 */
@Dao
interface DailyStandardEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DailyStandardEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DailyStandardEntryEntity)

    // Todas las entradas del día — base del IdentityScore
    @Query("SELECT * FROM daily_standard_entries WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay ORDER BY roleId ASC, standardType ASC")
    fun observeForDay(uID: String, startOfDay: Long, endOfDay: Long): Flow<List<DailyStandardEntryEntity>>

    @Query("SELECT * FROM daily_standard_entries WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay ORDER BY roleId ASC, standardType ASC")
    suspend fun getForDay(uID: String, startOfDay: Long, endOfDay: Long): List<DailyStandardEntryEntity>

    // Pendientes del día — para la UI de estándares y el worker de medianoche
    @Query("SELECT * FROM daily_standard_entries WHERE uID = :uID AND isCompleted = 0 AND date >= :startOfDay AND date < :endOfDay ORDER BY roleId ASC")
    fun observePendingForDay(uID: String, startOfDay: Long, endOfDay: Long): Flow<List<DailyStandardEntryEntity>>

    // Entradas de un rol específico — para el score por rol
    @Query("SELECT * FROM daily_standard_entries WHERE uID = :uID AND roleId = :roleId AND date >= :startOfDay AND date < :endOfDay")
    fun observeForDayByRole(uID: String, roleId: String, startOfDay: Long, endOfDay: Long): Flow<List<DailyStandardEntryEntity>>

    // Marcar completado manualmente
    @Query("UPDATE daily_standard_entries SET isCompleted = 1, completedAt = :completedAt, xpAwarded = :xpAwarded, isSynced = 0 WHERE id = :entryId")
    suspend fun markCompleted(entryId: String, completedAt: Long, xpAwarded: Int)

    // Auto-validación TRAINING — completa todos los TRAINING del día en una sola query
    @Query("UPDATE daily_standard_entries SET isCompleted = 1, completedAt = :completedAt, xpAwarded = :xpAwarded, autoValidated = 1, isSynced = 0 WHERE uID = :uID AND standardType = 'TRAINING' AND date >= :startOfDay AND date < :endOfDay AND isCompleted = 0")
    suspend fun autoValidateTraining(uID: String, startOfDay: Long, endOfDay: Long, completedAt: Long, xpAwarded: Int)

    @Query("UPDATE daily_standard_entries SET penaltyApplied = 1 WHERE id = :entryId")
    suspend fun markPenaltyApplied(entryId: String)

    // Incompletos sin penalización — para el worker de medianoche
    @Query("SELECT * FROM daily_standard_entries WHERE uID = :uID AND isCompleted = 0 AND penaltyApplied = 0 AND date >= :startOfDay AND date < :endOfDay")
    suspend fun getIncompleteForDay(uID: String, startOfDay: Long, endOfDay: Long): List<DailyStandardEntryEntity>

    @Query("SELECT * FROM daily_standard_entries WHERE isSynced = 0")
    suspend fun getUnsynced(): List<DailyStandardEntryEntity>

    @Query("UPDATE daily_standard_entries SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    // Guard de idempotencia — para generateTodayEntries()
    @Query("SELECT standardId FROM daily_standard_entries WHERE uID = :uID AND date >= :startOfDay AND date < :endOfDay")
    suspend fun getExistingStandardIdsForDay(uID: String, startOfDay: Long, endOfDay: Long): List<String>

    @Query("""
        UPDATE daily_standard_entries 
        SET isCompleted = 1, completedAt = :completedAt, xpAwarded = :xpAwarded,
            autoValidated = 1, isSynced = 0
        WHERE uID = :uID AND standardType = :standardType
        AND date >= :startOfDay AND date < :endOfDay
        AND isCompleted = 0
    """)
    suspend fun autoValidateByType(
        uID: String,
        startOfDay: Long,
        endOfDay: Long,
        completedAt: Long,
        xpAwarded: Int,
        standardType: String
    )

    // Marcar como fallido activamente (comió mal, evidencia directa)
    @Query("UPDATE daily_standard_entries SET isFailed = 1, isSynced = 0 WHERE id = :entryId")
    suspend fun markFailed(entryId: String)

    @Query("""
        UPDATE daily_standard_entries
        SET isFailed = 1, isSynced = 0
        WHERE uID = :uID AND standardType = :standardType
        AND date >= :startOfDay AND date < :endOfDay
        AND isCompleted = 0
    """)
    suspend fun markFailedByType(uID: String, standardType: String, startOfDay: Long, endOfDay: Long)

    // Marcar como fallido por standardId (cuando viene del UseCase con standardId)
    @Query("UPDATE daily_standard_entries SET isFailed = 1, isSynced = 0 WHERE uID = :uID AND standardId = :standardId AND date >= :startOfDay AND date < :endOfDay AND isCompleted = 0")
    suspend fun markFailedByStandardId(uID: String, standardId: String, startOfDay: Long, endOfDay: Long)
}