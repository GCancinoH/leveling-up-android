package com.gcancino.levelingup.domain.models.event

import com.gcancino.levelingup.domain.useCases.processors.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Procesador central de eventos.
 * TODOS los eventos del usuario pasan por este único punto.
 *
 * Esto garantiza:
 * - Consistencia en el procesamiento
 * - Un solo lugar para logging
 * - Un solo lugar para aplicar reglas de negocio
 * - Fácil debugging y testing
 */
@Singleton
class CentralEventProcessor @Inject constructor(
    private val processNutritionResultUseCase: ProcessNutritionResultUseCase,
    private val processTaskCompletionUseCase: ProcessTaskCompletionUseCase,
    private val processTrainingResultUseCase: ProcessTrainingResultUseCase,
    private val processMorningFlowUseCase: ProcessMorningFlowUseCase,
    private val processEveningFlowUseCase: ProcessEveningFlowUseCase,
    private val processQuestCompletionUseCase: ProcessQuestCompletionUseCase,
    private val processStreakUpdateUseCase: ProcessStreakUpdateUseCase
) {

    /**
     * Procesa cualquier evento del usuario.
     * Este es el ÚNICO punto de entrada para eventos.
     */
    suspend fun process(event: PlayerEvent) {
        when (event) {
            // Nutrition
            is PlayerEvent.NutritionAnalyzed -> processNutrition(event)

            // Tasks
            is PlayerEvent.TaskCompleted -> processTask(event)
            is PlayerEvent.TaskFailed -> processTaskFailed(event)

            // Training
            is PlayerEvent.TrainingCompleted -> processTraining(event)
            is PlayerEvent.TrainingFailed -> processTrainingFailed(event)

            // Morning Flow
            is PlayerEvent.MorningFlowCompleted -> processMorning(event)

            // Evening Flow
            is PlayerEvent.EveningFlowCompleted -> processEvening(event)

            // Quests
            is PlayerEvent.QuestCompleted -> processQuestCompleted(event)
            is PlayerEvent.QuestFailed -> processQuestFailed(event)

            // Streaks
            is PlayerEvent.StreakBroken -> processStreakBroken(event)
            is PlayerEvent.StreakExtended -> processStreakExtended(event)
        }
    }

    // ==================== NUTRITION ====================
    private suspend fun processNutrition(event: PlayerEvent.NutritionAnalyzed) {
        processNutritionResultUseCase.execute(event.nutritionEntry)
    }

    // ==================== TASKS ====================
    private suspend fun processTask(event: PlayerEvent.TaskCompleted) {
        processTaskCompletionUseCase.execute(event.taskId)
    }

    private suspend fun processTaskFailed(event: PlayerEvent.TaskFailed) {
        processTaskCompletionUseCase.fail(event.taskId, event.reason)
    }

    // ==================== TRAINING ====================
    private suspend fun processTraining(event: PlayerEvent.TrainingCompleted) {
        processTrainingResultUseCase.execute(
            sessionId = event.sessionID,
            durationSeconds = event.durationInSeconds,
            caloriesBurned = event.caloriesBurned
        )
    }

    private suspend fun processTrainingFailed(event: PlayerEvent.TrainingFailed) {
        processTrainingResultUseCase.fail(event.sessionID, event.reason)
    }

    // ==================== MORNING FLOW ====================
    private suspend fun processMorning(event: PlayerEvent.MorningFlowCompleted) {
        processMorningFlowUseCase.execute(
            answers = event.answers
        )
    }

    // ==================== EVENING FLOW ====================
    private suspend fun processEvening(event: PlayerEvent.EveningFlowCompleted) {
        processEveningFlowUseCase.execute(
            answers = event.answers
        )
    }

    // ==================== QUESTS ====================
    private suspend fun processQuestCompleted(event: PlayerEvent.QuestCompleted) {
        processQuestCompletionUseCase.execute(
            questId = event.questId,
            xpEarned = event.xpEarned,
            coinsEarned = event.coinsEarned
        )
    }

    private suspend fun processQuestFailed(event: PlayerEvent.QuestFailed) {
        processQuestCompletionUseCase.fail(event.questId, event.reason)
    }

    // ==================== STREAKS ====================
    private suspend fun processStreakBroken(event: PlayerEvent.StreakBroken) {
        processStreakUpdateUseCase.onStreakBroken(
            category = event.category,
            previousStreak = event.previousStreak
        )
    }

    private suspend fun processStreakExtended(event: PlayerEvent.StreakExtended) {
        processStreakUpdateUseCase.onStreakExtended(
            category = event.category,
            currentStreak = event.currentStreak
        )
    }
}