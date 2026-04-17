package com.gcancino.levelingup.domain.models.event

import com.gcancino.levelingup.domain.models.dailyTasks.ReflectionAnswer
import com.gcancino.levelingup.domain.models.nutrition.NutritionEntry

sealed class PlayerEvent {
    data class NutritionAnalyzed(
        val nutritionEntry: NutritionEntry,
        val uID: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class TaskCompleted(
        val taskId: String,
        val uID: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class TaskFailed(
        val taskId: String,
        val uID: String,
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class TrainingCompleted(
        val sessionID: String,
        val completedSetsCount: Int,    // ← necesario para calcular XP
        val uID: String,
        val durationInSeconds: Int = 0,
        val caloriesBurned: Int? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class TrainingFailed(
        val sessionID: String,
        val uID: String,
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class MorningFlowCompleted(
        val answers: List<MorningAnswer>,
        val uID: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class EveningFlowCompleted(
        val answers: List<EveningAnswer>,
        val uID: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class QuestCompleted(
        val questId: String,
        val xpEarned: Int,
        val coinsEarned: Int,
        val uID: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class QuestFailed(
        val questId: String,
        val uID: String,
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class StreakBroken(
        val category: String,
        val previousStreak: Int,
        val uID: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class StreakExtended(
        val category: String,
        val currentStreak: Int,
        val uID: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()
}

data class MorningAnswer(
    val questionId: String,
    val answer: String,
    val score: Int? = null
)

data class EveningAnswer(
    val questionId: String,
    val answer: String,
    val score: Int? = null
)