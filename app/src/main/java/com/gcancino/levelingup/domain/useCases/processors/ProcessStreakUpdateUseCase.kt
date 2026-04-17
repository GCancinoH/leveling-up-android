package com.gcancino.levelingup.domain.useCases.processors

import com.gcancino.levelingup.data.local.database.dao.PlayerStreakDao
import com.gcancino.levelingup.domain.logic.GeneratedQuestManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para procesar actualizaciones de streaks.
 */
@Singleton
class ProcessStreakUpdateUseCase @Inject constructor(
    private val playerStreakDao: PlayerStreakDao,
    private val generatedQuestManager: GeneratedQuestManager
) {
    private val TAG = "ProcessStreakUC"

    /**
     * Ejecuta el procesamiento cuando un streak se rompe.
     */
    suspend fun onStreakBroken(category: String, previousStreak: Int, uID: String) {
        Timber.tag(TAG).w(
            "💀 Racha rota en '$category' | era: $previousStreak días"
        )
        // El reset real lo hace DailyResetManager.handleCleanDay/evaluateDay
        // para garantizar idempotencia. Aquí solo logging.
    }

    /**
     * Ejecuta el procesamiento cuando un streak se extiende.
     */
    suspend fun onStreakExtended(category: String, currentStreak: Int, uID: String) {
        Timber.tag(TAG).i("🔥 Racha extendida en '$category' → $currentStreak días")
        // El milestone/token lo maneja DailyResetManager.handleCleanDay()
        // Este hook es para UI feedback inmediato solamente.
    }
}