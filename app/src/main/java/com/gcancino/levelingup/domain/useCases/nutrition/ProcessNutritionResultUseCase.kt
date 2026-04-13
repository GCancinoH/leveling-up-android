package com.gcancino.levelingup.domain.useCases.nutrition

import com.gcancino.levelingup.data.local.database.dao.DailyStandardEntryDao
import com.gcancino.levelingup.domain.logic.GeneratedQuestManager
import com.gcancino.levelingup.domain.logic.TimeProvider
import com.gcancino.levelingup.domain.models.dailyTasks.DailyTask
import com.gcancino.levelingup.domain.models.dailyTasks.TaskPriority
import com.gcancino.levelingup.domain.models.nutrition.NutritionActionType
import com.gcancino.levelingup.domain.models.nutrition.NutritionEntry
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.repositories.IdentityRepository
import timber.log.Timber
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class ProcessNutritionResultUseCase @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val dailyTasksRepository: DailyTasksRepository,
    private val standardEntryDao: DailyStandardEntryDao,
    private val generatedQuestManager: GeneratedQuestManager,
    private val timeProvider: TimeProvider
) {

    private val TAG = "ProcessNutritionUseCase"

    suspend fun execute(entry: NutritionEntry, uID: String) {
        val (start, end) = timeProvider.dayBoundaries(timeProvider.today())
        val standardId   = entry.action?.standardId

        // ── 1. PROBLEMA 1 FIX: markCompleted OR markFailed ───────────────────────
        if (standardId != null) {
            if (entry.alignmentScore >= 0.7f) {
                // Completado — comió bien
                val dbEntry = standardEntryDao.getForDay(uID, start, end)
                    .firstOrNull { it.standardId == standardId }

                if (dbEntry != null && !dbEntry.isCompleted) {
                    standardEntryDao.markCompleted(
                        entryId     = dbEntry.id,
                        completedAt = System.currentTimeMillis(),
                        xpAwarded   = dbEntry.xpAwarded
                    )
                    Timber.tag(TAG).d("Standard COMPLETED → ${dbEntry.standardTitle}")
                }
            } else {
                // Fallido activamente — comió mal con evidencia directa
                standardEntryDao.markFailedByStandardId(uID, standardId, start, end)
                Timber.tag(TAG).d("Standard FAILED → $standardId | score: ${entry.alignmentScore}")
            }
        } else if (entry.alignmentScore >= 0.7f) {
            // Sin standardId específico pero alineado → validar todos NUTRITION
            identityRepository.autoValidateNutrition(uID)
        }
        // Si score < 0.7 y no hay standardId → no marcar nada (sigue pendiente)
        // La penalización la aplica DailyResetManager a medianoche

        // ── 2. PROBLEMA 3 FIX: quest fallback sin standardId ─────────────────────
        if (standardId != null) {
            generatedQuestManager.evaluateNutritionImpact(uID, standardId)
        } else {
            // Fallback: evaluar por score general si hay quest de tipo NUTRITION
            generatedQuestManager.evaluateGeneralNutrition(uID, entry.alignmentScore)
        }

        // ── 3. Acción estructurada ────────────────────────────────────────────────
        if (entry.action?.type == NutritionActionType.ADD_TASK) {
            val taskTitle = entry.action.taskTitle ?: return
            dailyTasksRepository.saveTasks(listOf(
                DailyTask(
                    id       = UUID.randomUUID().toString(),
                    uID      = uID,
                    date     = Date(),
                    title    = taskTitle,
                    priority = TaskPriority.INTERMEDIATE,
                    xpReward = 5,
                    isSynced = false
                )
            ))
        }
    }

    // Actualizar estándar específico por ID 
    // Si el score ≥ 0.7 → marcar como completado
    // Si el score < 0.7 → no marcar (la entrada queda pendiente para penalización)
    private suspend fun updateSpecificStandard(
        uID: String,
        standardId: String,
        alignmentScore: Float
    ) {
        val (start, end) = timeProvider.dayBoundaries(timeProvider.today())

        val entry = standardEntryDao.getForDay(uID, start, end)
            .firstOrNull { it.standardId == standardId }

        if (entry == null) {
            Timber.tag(TAG).w("StandardEntry no encontrada para standardId: $standardId")
            return
        }

        if (entry.isCompleted) {
            Timber.tag(TAG).d("Estándar '$standardId' ya completado — skip")
            return
        }

        if (alignmentScore >= 0.7f) {
            standardEntryDao.markCompleted(
                entryId     = entry.id,
                completedAt = System.currentTimeMillis(),
                xpAwarded   = entry.xpAwarded
            )
            Timber.tag(TAG).d(
                "Estándar NUTRITION completado → %s | score: %f",
                entry.standardTitle, alignmentScore
            )
        } else {
            Timber.tag(TAG).d(
                "Estándar NUTRITION NO completado → %s | score: %f < 0.7",
                entry.standardTitle, alignmentScore
            )
        }
    }
}