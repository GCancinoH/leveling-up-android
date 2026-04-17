package com.gcancino.levelingup.domain.useCases.processors

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.logic.DailyResetManager
import com.gcancino.levelingup.domain.repositories.IdentityRepository
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para procesar resultados de entrenamiento.
 */
@Singleton
class ProcessTrainingResultUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val identityRepository: IdentityRepository,
    private val dailyResetManager: DailyResetManager
) {

    private val TAG = "ProcessTrainingUC"

    data class Result(val xpEarned: Int, val newLevel: Int)

    /**
     * Ejecuta el procesamiento completo de un entrenamiento completado.
     */
    suspend fun execute(
        sessionId: String,
        completedSetsCount: Int,
        uID: String,
        durationSeconds: Int = 0,
        caloriesBurned: Int? = null
    ): Resource<Result> {
        val xpToAward = completedSetsCount * XP_PER_SET

        val xpResult = playerRepository.awardXP(uID, xpToAward)
        if (xpResult is Resource.Error) {
            Timber.tag(TAG).e("XP award failed: ${xpResult.message}")
            return Resource.Error(xpResult.message ?: "Failed to award XP")
        }
        val newLevel = (xpResult as Resource.Success).data ?: 1

        // Auto-validar estándar TRAINING — non-fatal si falla
        when (val valResult = identityRepository.autoValidateTraining(uID)) {
            is Resource.Error -> Timber.tag(TAG).w(
                "Training standard validation failed (non-fatal): ${valResult.message}"
            )
            else -> Timber.tag(TAG).d("✔ TRAINING standard auto-validated")
        }

        Timber.tag(TAG).i(
            "✔ Training completed → $completedSetsCount sets | " +
                    "+$xpToAward XP | nivel: $newLevel | duración: ${durationSeconds}s"
        )
        return Resource.Success(Result(xpEarned = xpToAward, newLevel = newLevel))
    }

    /**
     * Procesa el fallo de un entrenamiento.
     */
    suspend fun fail(sessionId: String, reason: String?, uID: String) {
        Timber.tag(TAG).w("Training $sessionId falló | razón: $reason")
        // Marcar estándar TRAINING como isFailed para que IdentityScore lo refleje
        // DailyResetManager aplicará la penalización completa (XP + streak) a medianoche
        when (val result = identityRepository.markTrainingFailed(uID)) {
            is Resource.Error -> Timber.tag(TAG).e("markTrainingFailed failed: ${result.message}")
            else -> Timber.tag(TAG).d("✔ TRAINING standard marcado como isFailed")
        }
    }

    companion object {
        const val XP_PER_SET = 10
    }
}