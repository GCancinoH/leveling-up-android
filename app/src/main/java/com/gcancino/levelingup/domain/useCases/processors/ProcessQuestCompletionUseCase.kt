package com.gcancino.levelingup.domain.useCases.processors

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.logic.GeneratedQuestManager
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para procesar completado de quests.
 */
@Singleton
class ProcessQuestCompletionUseCase @Inject constructor(
    private val generatedQuestManager: GeneratedQuestManager,
    private val playerRepository: PlayerRepository
) {
    private val TAG = "ProcessQuestUC"

    /**
     * Ejecuta el procesamiento completo de una quest completada.
     */
    suspend fun execute(questId: String, xpEarned: Int, coinsEarned: Int, uID: String) {
        if (xpEarned > 0) {
            when (val result = playerRepository.awardXP(uID, xpEarned)) {
                is Resource.Error -> Timber.tag(TAG).e("XP award failed: ${result.message}")
                is Resource.Success -> Timber.tag(TAG).i(
                    "✔ Quest $questId completada → +$xpEarned XP | nivel: ${result.data}"
                )
                else -> Unit
            }
        }
    }

    /**
     * Procesa el fallo de una quest.
     */
    suspend fun fail(questId: String, reason: String?, uID: String) {
        Timber.tag(TAG).w("Quest $questId fallida | razón: $reason")
        // GeneratedQuestManager.evaluateProgress() ya maneja EXPIRED/FAILED
        // automáticamente en el ciclo nocturno. Este hook es para fallos
        // explícitos fuera del ciclo normal (ej. el usuario rinde la quest).
    }
}