package com.gcancino.levelingup.domain.use_cases.dailyTasks

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.dailyTasks.MorningEntry
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import javax.inject.Inject

class SaveMorningEntryUseCase @Inject constructor(
    private val dailyTaskRepository: DailyTasksRepository,
    private val playerRepository: PlayerRepository
) {
    suspend operator fun invoke(uID: String, entry: MorningEntry): Resource<Int> {
        // Save the entry
        val saveEntryResult = dailyTaskRepository.saveMorningEntry(entry)
        if (saveEntryResult is Resource.Error) {
            return Resource.Error(saveEntryResult.message ?: "Failed to save morning entry")
        }

        // Award XP
        return playerRepository.awardXP(uID, 10)
    }
}