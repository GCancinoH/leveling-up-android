package com.gcancino.levelingup.domain.useCases.processors

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UseCase para procesar completado de tareas.
 *
 * Regla de negocio: NINGÚN ViewModel toma decisiones de negocio.
 */
@Singleton
class ProcessTaskCompletionUseCase @Inject constructor(
    private val dailyTasksRepository: DailyTasksRepository,
) {
    private val TAG = "ProcessTaskUC"

    data class Result(val xpEarned: Int, val newLevel: Int)
    /**
     * Ejecuta el procesamiento completo de una tarea completada.
     */
    suspend fun execute(taskId: String, uID: String): Resource<Result> {
        return when (val r = dailyTasksRepository.completeTask(taskId, uID)) {
            is Resource.Success -> {
                Timber.tag(TAG).i("✔ Task completada → nivel: ${r.data}")
                Resource.Success(Result(xpEarned = 0, newLevel = r.data ?: 1))
            }
            is Resource.Error -> Resource.Error(r.message ?: "Failed to complete task")
            else -> Resource.Error("Unexpected state")
        }
    }

    /**
     * Procesa el fallo de una tarea.
     */
    suspend fun fail(taskId: String, reason: String?, uID: String) {
        // Solo logging — la penalización la aplica DailyResetManager
        Timber.tag(TAG).w("Task $taskId marcada como fallida | razón: $reason")
    }
}