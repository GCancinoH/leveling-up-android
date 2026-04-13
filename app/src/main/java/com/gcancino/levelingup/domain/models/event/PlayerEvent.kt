package com.gcancino.levelingup.domain.models.event

import com.gcancino.levelingup.domain.models.dailyTasks.ReflectionAnswer
import com.gcancino.levelingup.domain.models.nutrition.NutritionEntry

sealed class PlayerEvent {
    // Nutrition Events
    data class NutritionAnalyzed(
        val nutritionEntry: NutritionEntry,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    // Task Events
    data class TaskCompleted(
        val taskId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class TaskFailed(
        val taskId: String,
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    // Training Events
    data class TrainingCompleted(
        val sessionID: String,
        val durationInSeconds: Int,
        val caloriesBurned: Int? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class TrainingFailed(
        val sessionID: String,
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    // Morning Flow
    data class MorningFlowCompleted(
        val answers: List<MorningAnswer>,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class MorningFlowFailed(
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Evening Flow
    data class EveningFlowCompleted(
        val answers: List<EveningAnswer>,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class EveningFlowFailed(
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Quest Events
    data class QuestCompleted(
        val questId: String,
        val xpEarned: Int,
        val coinsEarned: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class QuestFailed(
        val questId: String,
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    // Streak Events
    data class StreakBroken(
        val category: String,
        val previousStreak: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : PlayerEvent()

    data class StreakExtended(
        val category: String,
        val currentStreak: Int,
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