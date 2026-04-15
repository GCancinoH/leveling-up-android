package com.gcancino.levelingup.domain.useCases.processors

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.logic.GeneratedQuestManager
import com.gcancino.levelingup.domain.logic.TimeProvider
import com.gcancino.levelingup.domain.models.event.MorningAnswer
import com.gcancino.levelingup.domain.models.event.PlayerEvent
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para procesar el flujo matutino completado.
 *
 * Regla de negocio: NINGÚN ViewModel toma decisiones de negocio.
 * Este UseCase ejecuta TODA la lógica cuando el usuario completa el morning flow.
 */
@Singleton
class ProcessMorningFlowUseCase @Inject constructor(
    private val dailyTasksRepository: DailyTasksRepository,
    private val playerRepository: PlayerRepository,
    private val generatedQuestManager: GeneratedQuestManager,
    private val timeProvider: TimeProvider
) {

    private val TAG = "ProcessMorningFlowUC"

    /**
     * Ejecuta el procesamiento completo del flujo matutino.
     *
     * Acciones:
     * 1. Validar respuestas (mínimo 1 respuesta)
     * 2. Award XP por completar el ritual matutino
     * 3. Actualizar streak de morning flow
     * 4. Evaluar impacto en quests generadas
     * 5. Loguear evento para tracking futuro
     */
    suspend fun execute(
        uID: String,
        answers: List<MorningAnswer>
    ): Result {

        // 1. Validar respuestas
        if (answers.isEmpty()) {
            Timber.tag(TAG).w("Morning flow sin respuestas → reject")
            return Result.Failure("No answers provided")
        }

        val today = timeProvider.today()
        val (start, end) = timeProvider.dayBoundaries(today)

        // 2. Award XP por completar el ritual (10 XP base)
        val xpReward = 10
        val newLevel = when (val awardResult = playerRepository.awardXP(uID, xpReward)) {
            is Resource.Success -> awardResult.data ?: 1
            is Resource.Error -> {
                Timber.tag(TAG).e("Failed to award XP: ${awardResult.message}")
                return Result.Failure("Failed to award XP: ${awardResult.message}")
            }
            is Resource.Loading -> null
        }

        Timber.tag(TAG).d("✔ Morning flow completed → +$xpReward XP | Level: $newLevel")

        // 3. Evaluar quests generadas (si hay quests activas de tipo MORNING/CONSISTENCY)
        generatedQuestManager.evaluateProgress(uID)

        // 4. Preparar datos para logging
        val answerCount = answers.size
        val hasHighIntent = answers.any {
            it.answer.contains("alto", ignoreCase = true) ||
                    it.answer.contains("prioridad", ignoreCase = true) ||
                    it.answer.length > 100
        }

        Timber.tag(TAG).i(
            "✔ Morning flow procesado → $answerCount respuestas | " +
                    "high_intent: $hasHighIntent | XP: +$xpReward"
        )

        return Result.Success(
            xpEarned = xpReward,
            newLevel = newLevel!!,
            answerCount = answerCount,
            hasHighIntent = hasHighIntent
        )
    }

    /**
     * Resultado del procesamiento del morning flow.
     */
    data class Success(
        val xpEarned: Int,
        val newLevel: Int,
        val answerCount: Int,
        val hasHighIntent: Boolean
    )

    sealed class Result {
        data class Success(
            val xpEarned: Int,
            val newLevel: Int,
            val answerCount: Int,
            val hasHighIntent: Boolean
        ) : Result()
        data class Failure(val reason: String) : Result()
    }
}