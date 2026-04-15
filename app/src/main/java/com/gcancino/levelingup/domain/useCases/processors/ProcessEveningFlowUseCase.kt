package com.gcancino.levelingup.domain.useCases.processors

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.logic.GeneratedQuestManager
import com.gcancino.levelingup.domain.logic.TimeProvider
import com.gcancino.levelingup.domain.models.event.EveningAnswer
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para procesar el flujo nocturno completado.
 *
 * Regla de negocio: NINGÚN ViewModel toma decisiones de negocio.
 * Este UseCase ejecuta TODA la lógica cuando el usuario completa el evening flow.
 */
@Singleton
class ProcessEveningFlowUseCase @Inject constructor(
    private val dailyTasksRepository: DailyTasksRepository,
    private val playerRepository: PlayerRepository,
    private val generatedQuestManager: GeneratedQuestManager,
    private val timeProvider: TimeProvider
) {

    private val TAG = "ProcessEveningFlowUC"

    /**
     * Ejecuta el procesamiento completo del flujo nocturno.
     *
     * Acciones:
     * 1. Validar respuestas (mínimo 1 respuesta)
     * 2. Award XP por completar el ritual nocturno
     * 3. Analizar calidad de reflexión (longitud, profundidad)
     * 4. Evaluar impacto en quests generadas
     * 5. Preparar insights para el próximo día
     * 6. Loguear evento para tracking futuro
     */
    suspend fun execute(
        uID: String,
        answers: List<EveningAnswer>
    ): Result {

        // 1. Validar respuestas
        if (answers.isEmpty()) {
            Timber.tag(TAG).w("Evening flow sin respuestas → reject")
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

        Timber.tag(TAG).d("✔ Evening flow completed → +$xpReward XP | Level: $newLevel")

        // 3. Analizar calidad de reflexión
        val answerCount = answers.size
        val totalLength = answers.sumOf { it.answer.length }
        val avgLength = totalLength / answerCount

        val reflectionQuality = when {
            avgLength >= 150 -> ReflectionQuality.DEEP      // >150 chars promedio = reflexión profunda
            avgLength >= 80  -> ReflectionQuality.MODERATE  // 80-150 chars = moderada
            else             -> ReflectionQuality.SURFACE   // <80 chars = superficial
        }

        // 4. Evaluar quests generadas
        generatedQuestManager.evaluateProgress(uID)

        // 5. Detectar patrones para insights (keywords comunes)
        val hasGratitude = answers.any {
            it.answer.contains("gracias", ignoreCase = true) ||
                    it.answer.contains("agradec", ignoreCase = true)
        }

        val hasLearning = answers.any {
            it.answer.contains("aprend", ignoreCase = true) ||
                    it.answer.contains("lección", ignoreCase = true) ||
                    it.answer.contains("mejorar", ignoreCase = true)
        }

        val hasPlanning = answers.any {
            it.answer.contains("mañana", ignoreCase = true) ||
                    it.answer.contains("plan", ignoreCase = true) ||
                    it.answer.contains("objetivo", ignoreCase = true)
        }

        Timber.tag(TAG).i(
            "✔ Evening flow procesado → $answerCount respuestas | " +
                    "quality: $reflectionQuality | gratitude: $hasGratitude | " +
                    "learning: $hasLearning | planning: $hasPlanning"
        )

        return Result.Success(
            xpEarned = xpReward,
            newLevel = newLevel!!,
            answerCount = answerCount,
            reflectionQuality = reflectionQuality,
            hasGratitude = hasGratitude,
            hasLearning = hasLearning,
            hasPlanning = hasPlanning
        )
    }

    /**
     * Calidad de la reflexión basada en longitud y contenido.
     */
    enum class ReflectionQuality {
        SURFACE,    // Reflexión rápida, poco detalle
        MODERATE,   // Reflexión adecuada
        DEEP        // Reflexión profunda, detallada
    }

    /**
     * Resultado del procesamiento del evening flow.
     */
    sealed class Result {
        data class Success(
            val xpEarned: Int,
            val newLevel: Int,
            val answerCount: Int,
            val reflectionQuality: ReflectionQuality,
            val hasGratitude: Boolean,
            val hasLearning: Boolean,
            val hasPlanning: Boolean
        ) : Result()
        data class Failure(val reason: String) : Result()
    }
}