package com.gcancino.levelingup.domain.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.exercise.OneRepMax
import com.gcancino.levelingup.domain.models.exercise.TrainingSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Date

interface ExerciseRepository {
    fun getTodaysSession(): Flow<Resource<TrainingSession?>>
    fun getSessionForDate(date: Date): Flow<Resource<TrainingSession?>>
    fun getOneRepMaxes(): Flow<Resource<List<OneRepMax>>>
    suspend fun syncWeek(date: LocalDate): Resource<Unit>
    suspend fun toggleSetLog(sessionId: String, exerciseId: String, setIndex: Int, isCompleted: Boolean)
    fun getCompletedSetsForSession(sessionId: String): Flow<Set<Int>> // Simplified for specific exercise
    // Add to ExerciseRepository interface
    fun getLogsForSessionAsMap(sessionId: String): Flow<Map<String, Set<Int>>>
    suspend fun saveTodaySessionLocally(session: TrainingSession): Resource<Unit>
}
