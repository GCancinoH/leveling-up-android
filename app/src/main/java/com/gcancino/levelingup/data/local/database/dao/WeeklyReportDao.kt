package com.gcancino.levelingup.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gcancino.levelingup.data.local.database.entities.WeeklyReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: WeeklyReportEntity)

    // El más reciente primero — para la Identity Wall
    @Query("SELECT * FROM weekly_reports WHERE uID = :uID ORDER BY weekStart DESC LIMIT 8")
    fun observeRecent(uID: String): Flow<List<WeeklyReportEntity>>

    // Para el reporte de esta semana
    @Query("""
        SELECT * FROM weekly_reports 
        WHERE uID = :uID AND weekStart >= :start AND weekStart < :end 
        LIMIT 1
    """)
    suspend fun getForWeek(uID: String, start: Long, end: Long): WeeklyReportEntity?

    @Query("SELECT * FROM weekly_reports WHERE isSynced = 0")
    suspend fun getUnsynced(): List<WeeklyReportEntity>

    @Query("UPDATE weekly_reports SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}