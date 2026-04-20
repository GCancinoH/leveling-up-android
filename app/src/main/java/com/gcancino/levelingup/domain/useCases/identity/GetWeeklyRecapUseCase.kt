package com.gcancino.levelingup.domain.useCases.identity

import com.gcancino.levelingup.domain.models.dailyTasks.WeeklyRecap
import com.gcancino.levelingup.domain.models.identity.ObjectiveStatus
import com.gcancino.levelingup.domain.models.identity.TimeHorizon
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.repositories.ObjectiveRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.Calendar
import javax.inject.Inject

class GetWeeklyRecapUseCase @Inject constructor(
    private val tasksRepository: DailyTasksRepository,
    private val objectiveRepository: ObjectiveRepository
) {
    suspend operator fun invoke(uID: String): WeeklyRecap {
        val today = LocalDate.now()
        val weekNumber = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val year = today.year

        // 1. Fetch Objectives for this week
        val allObjectives = objectiveRepository.observeObjectivesByHorizon(uID, TimeHorizon.WEEK).first()
        val completed = allObjectives.filter { it.status == ObjectiveStatus.COMPLETED }
        val pending = allObjectives.filter { it.status == ObjectiveStatus.ACTIVE }

        // 2. Fetch Daily Wins (Extract from Evening Entries or specific tasks)
        // For now, let's assume we extract them from the DailyTask repository 
        // using the "last 7 days" logic.
        val recentTasks = tasksRepository.observeTasksForDate(uID, java.util.Date()).first() 
        // Note: Real implementation would query the specific range from the DB.
        
        return WeeklyRecap(
            weekNumber = weekNumber,
            year = year,
            dailyWins = emptyList(), // Placeholder for extraction logic
            challengesFaced = emptyList(),
            completedObjectives = completed,
            pendingObjectives = pending,
            totalXpGained = 0,
            identityAlignmentScore = if (allObjectives.isNotEmpty()) completed.size.toFloat() / allObjectives.size else 0f
        )
    }
}
