package com.gcancino.levelingup.domain.useCases.processors

import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para procesar actualizaciones de streaks.
 */
@Singleton
class ProcessStreakUpdateUseCase @Inject constructor(
    // private val playerRepository: PlayerRepository,
    // private val questRepository: QuestRepository
) {

    /**
     * Ejecuta el procesamiento cuando un streak se rompe.
     */
    suspend fun onStreakBroken(category: String, previousStreak: Int) {
        // TODO: Implementar lógica completa
        // 1. Registrar ruptura de streak
        // 2. Aplicar penalización si corresponde
        // 3. Notificar al usuario (feedback inmediato)
        // 4. Posiblemente crear penalty quest
        // 5. Loguear evento

        println("Streak broken in $category. Previous streak: $previousStreak")
    }

    /**
     * Ejecuta el procesamiento cuando un streak se extiende.
     */
    suspend fun onStreakExtended(category: String, currentStreak: Int) {
        // TODO: Implementar lógica completa
        // 1. Actualizar streak en repositorio
        // 2. Calcular bonus por racha (XP extra)
        // 3. Verificar milestones (7, 30, 100 días)
        // 4. Notificar al usuario con feedback positivo
        // 5. Loguear evento

        println("Streak extended in $category. Current streak: $currentStreak")
    }
}