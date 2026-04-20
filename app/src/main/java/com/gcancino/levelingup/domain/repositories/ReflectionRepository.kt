package com.gcancino.levelingup.domain.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.dailyTasks.WeeklyEntry
import kotlinx.coroutines.flow.Flow

interface ReflectionRepository {
    // Weekly Flow
    suspend fun saveWeeklyEntry(entry: WeeklyEntry): Resource<Unit>
    fun observeWeeklyEntry(uID: String, year: Int, weekNumber: Int): Flow<WeeklyEntry?>
    fun observeAllWeeklyEntries(uID: String): Flow<List<WeeklyEntry>>
    suspend fun syncUnsynced(): Resource<Unit>
}
