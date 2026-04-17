package com.gcancino.levelingup.domain.models.event

import com.gcancino.levelingup.domain.useCases.processors.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CentralEventProcessor @Inject constructor(
    private val processNutritionResult: ProcessNutritionResultUseCase,
    private val processTaskCompletion: ProcessTaskCompletionUseCase,
    private val processTrainingResult: ProcessTrainingResultUseCase,
    private val processMorningFlow: ProcessMorningFlowUseCase,
    private val processEveningFlow: ProcessEveningFlowUseCase,
    private val processQuestCompletion: ProcessQuestCompletionUseCase,
    private val processStreakUpdate: ProcessStreakUpdateUseCase
) {
    private val TAG = "CentralEventProcessor"

    suspend fun process(event: PlayerEvent) {
        Timber.tag(TAG).d("Processing: ${event::class.simpleName}")
        when (event) {
            is PlayerEvent.NutritionAnalyzed  -> processNutritionResult.execute(event.nutritionEntry)
            is PlayerEvent.TaskCompleted      -> processTaskCompletion.execute(event.taskId, event.uID)
            is PlayerEvent.TaskFailed         -> processTaskCompletion.fail(event.taskId, event.reason, event.uID)
            is PlayerEvent.TrainingCompleted  -> processTrainingResult.execute(
                sessionId          = event.sessionID,
                completedSetsCount = event.completedSetsCount,
                uID                = event.uID,
                durationSeconds    = event.durationInSeconds,
                caloriesBurned     = event.caloriesBurned
            )
            is PlayerEvent.TrainingFailed     -> processTrainingResult.fail(event.sessionID, event.reason, event.uID)
            is PlayerEvent.MorningFlowCompleted -> processMorningFlow.execute(event.uID, event.answers)
            is PlayerEvent.EveningFlowCompleted -> processEveningFlow.execute(event.uID, event.answers)
            is PlayerEvent.QuestCompleted     -> processQuestCompletion.execute(event.questId, event.xpEarned, event.coinsEarned, event.uID)
            is PlayerEvent.QuestFailed        -> processQuestCompletion.fail(event.questId, event.reason, event.uID)
            is PlayerEvent.StreakBroken       -> processStreakUpdate.onStreakBroken(event.category, event.previousStreak, event.uID)
            is PlayerEvent.StreakExtended     -> processStreakUpdate.onStreakExtended(event.category, event.currentStreak, event.uID)
        }
    }
}