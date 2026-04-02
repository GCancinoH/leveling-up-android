package com.gcancino.levelingup.domain.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.dailyTasks.DailyTask
import com.gcancino.levelingup.domain.models.dailyTasks.EveningEntry
import com.gcancino.levelingup.domain.models.dailyTasks.MorningEntry
import com.gcancino.levelingup.domain.models.dailyTasks.PenaltyEvent
import kotlinx.coroutines.flow.Flow

interface DailyTasksRepository {
    // ── Morning ───────────────────────────────────────────────────────────────────
    suspend fun saveMorningEntry(entry: MorningEntry): Resource<Unit>
    fun observeMorningCompletedToday(uID: String): Flow<Boolean>
    suspend fun getTodaysMorningEntry(uID: String): MorningEntry?

    // ── Evening ───────────────────────────────────────────────────────────────────
    suspend fun saveEveningEntry(entry: EveningEntry): Resource<Unit>
    fun observeEveningCompletedToday(uID: String): Flow<Boolean>

    // ── Tasks ─────────────────────────────────────────────────────────────────────
    suspend fun saveTasks(tasks: List<DailyTask>): Resource<Unit>
    fun getTodaysTasks(uID: String): Flow<List<DailyTask>>
    fun getTodaysPendingTasks(uID: String): Flow<List<DailyTask>>
    suspend fun completeTask(taskId: String, uID: String): Resource<Int>   // returns XP earned
    suspend fun countTodaysTasks(uID: String): Int

    // ── Penalty ───────────────────────────────────────────────────────────────────
    suspend fun applyMidnightPenalty(uID: String): Resource<PenaltyEvent?>
    suspend fun getLatestPenalty(uID: String): PenaltyEvent?

    // ── Sync ─────────────────────────────────────────────────────────────────────
    suspend fun syncUnsynced(): Resource<Unit>
}
