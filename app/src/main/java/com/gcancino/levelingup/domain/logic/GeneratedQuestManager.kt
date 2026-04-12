package com.gcancino.levelingup.domain.logic

import com.gcancino.levelingup.data.local.database.dao.DailyStandardEntryDao
import com.gcancino.levelingup.data.local.database.dao.GeneratedQuestDao
import com.gcancino.levelingup.data.local.database.dao.PlayerProgressDao
import com.gcancino.levelingup.data.local.database.entities.identity.GeneratedQuestEntity
import com.gcancino.levelingup.domain.models.identity.GeneratedQuestStatus
import com.gcancino.levelingup.domain.models.identity.GeneratedQuestType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saves LLM-generated quests from WeeklySyncWorker and
 * evaluates their progress on every app open via DailyResetManager.
 */
@Singleton
class GeneratedQuestManager @Inject constructor(
    private val questDao: GeneratedQuestDao,
    private val standardEntryDao: DailyStandardEntryDao,
    private val playerProgressDao: PlayerProgressDao,
    private val timeProvider: TimeProvider,
    private val gson: Gson
) {

    private val TAG = "GeneratedQuestManager"

    // ── Save quest from weekly report ──────────────────────────────────────────
    // Called from WeeklySyncWorker after receiving the Flask response.

    suspend fun saveQuestFromReport(
        uID: String,
        weeklyReportId: String,
        questJson: Map<String, Any>
    ) {
        try {
            // Don't create a new quest if one is already active
            val existing = questDao.getActive(uID)
            if (existing != null) {
                Timber.tag(TAG).d("Active quest already exists — skipping new quest creation")
                return
            }

            val title       = questJson["title"] as? String ?: return
            val description = questJson["description"] as? String ?: ""
            val typeStr     = questJson["type"] as? String ?: "CONSISTENCY"
            val targetIds   = (questJson["target_standard_ids"] as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            val goal        = (questJson["goal"] as? Number)?.toInt() ?: 5
            val duration    = (questJson["duration_days"] as? Number)?.toInt() ?: 7

            val startDate = Date()
            val endDate   = Date(startDate.time + duration * 24 * 60 * 60 * 1000L)

            questDao.insert(
                GeneratedQuestEntity(
                    id                = UUID.randomUUID().toString(),
                    uID               = uID,
                    weeklyReportId    = weeklyReportId,
                    title             = title,
                    description       = description,
                    type              = typeStr,
                    targetStandardIds = gson.toJson(targetIds),
                    goal              = goal,
                    durationDays      = duration,
                    currentProgress   = 0,
                    status            = "ACTIVE",
                    startDate         = startDate,
                    endDate           = endDate,
                    xpReward          = 150,
                    isSynced          = false
                )
            )
            Timber.tag(TAG).i("✔ Generated quest saved → '$title' | type: $typeStr | $duration days")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "saveQuestFromReport() failed")
        }
    }

    // ── Evaluate active quest progress ─────────────────────────────────────────
    // Called from DailyResetManager on every app open.
    // Checks if the quest's target standards were completed yesterday.

    suspend fun evaluateProgress(uID: String) {
        val quest = questDao.getActive(uID) ?: return

        val today     = timeProvider.today()
        val yesterday = today.minusDays(1)

        // Quest expired?
        val endDate = quest.endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        if (yesterday.isAfter(endDate)) {
            val finalStatus = if (quest.currentProgress >= quest.goal)
                GeneratedQuestStatus.COMPLETED else GeneratedQuestStatus.EXPIRED
            questDao.updateStatus(
                quest.id,
                finalStatus.name,
                if (finalStatus == GeneratedQuestStatus.COMPLETED) System.currentTimeMillis() else null
            )
            if (finalStatus == GeneratedQuestStatus.COMPLETED) {
                awardQuestXP(uID, quest.xpReward)
                Timber.tag(TAG).i("✔ Quest '${quest.title}' COMPLETED → +${quest.xpReward} XP")
            } else {
                Timber.tag(TAG).i("Quest '${quest.title}' EXPIRED (${quest.currentProgress}/${quest.goal})")
            }
            return
        }

        val targetIds: List<String> = gson.fromJson(
            quest.targetStandardIds,
            object : TypeToken<List<String>>() {}.type
        )

        val (start, end) = timeProvider.dayBoundaries(yesterday)
        val entries      = standardEntryDao.getForDay(uID, start, end)
            .filter { it.standardId in targetIds }

        val type    = GeneratedQuestType.valueOf(quest.type)
        val success = when (type) {
            GeneratedQuestType.STREAK       -> entries.all { it.isCompleted }
            GeneratedQuestType.CONSISTENCY  -> entries.any { it.isCompleted }
            GeneratedQuestType.ELIMINATION  -> entries.all { it.isCompleted } // no failures
        }

        if (success) {
            val newProgress = quest.currentProgress + 1
            questDao.updateProgress(quest.id, newProgress)
            Timber.tag(TAG).d("Quest progress → ${newProgress}/${quest.goal}")

            // Check completion
            if (newProgress >= quest.goal) {
                questDao.updateStatus(
                    quest.id,
                    GeneratedQuestStatus.COMPLETED.name,
                    System.currentTimeMillis()
                )
                awardQuestXP(uID, quest.xpReward)
                Timber.tag(TAG).i("✔ Quest '${quest.title}' COMPLETED early → +${quest.xpReward} XP")
            }
        } else {
            // STREAK fails on any miss — reset progress
            if (type == GeneratedQuestType.STREAK) {
                questDao.updateProgress(quest.id, 0)
                Timber.tag(TAG).d("STREAK quest reset — missed yesterday")
            }
        }
    }

    suspend fun evaluateNutritionImpact(uID: String, standardId: String) {
        val quest = questDao.getActive(uID) ?: return

        val targetIds: List<String> = gson.fromJson(
            quest.targetStandardIds,
            object : TypeToken<List<String>>() {}.type
        )

        // Si el estándar no es target de esta quest, no hacer nada
        if (standardId !in targetIds) return

        val today          = timeProvider.today()
        val (start, end)   = timeProvider.dayBoundaries(today)
        val entries        = standardEntryDao.getForDay(uID, start, end)
            .filter { it.standardId in targetIds }

        val type    = GeneratedQuestType.valueOf(quest.type)
        val success = when (type) {
            GeneratedQuestType.STREAK      -> entries.all { it.isCompleted }
            GeneratedQuestType.CONSISTENCY -> entries.any { it.isCompleted }
            GeneratedQuestType.ELIMINATION -> entries.all { it.isCompleted }
        }

        if (success) {
            val newProgress = quest.currentProgress + 1
            questDao.updateProgress(quest.id, newProgress)
            Timber.tag(TAG).d(
                "Quest progress (nutrition impact) → ${newProgress}/${quest.goal}"
            )

            if (newProgress >= quest.goal) {
                questDao.updateStatus(
                    quest.id,
                    GeneratedQuestStatus.COMPLETED.name,
                    System.currentTimeMillis()
                )
                awardQuestXP(uID, quest.xpReward)
                Timber.tag(TAG).i("✔ Quest completada por impacto nutricional → +${quest.xpReward} XP")
            }
        }
    }

    suspend fun evaluateGeneralNutrition(uID: String, alignmentScore: Float) {
        val quest = questDao.getActive(uID) ?: return

        val targetIds: List<String> = gson.fromJson(
            quest.targetStandardIds,
            object : TypeToken<List<String>>() {}.type
        )

        // Solo actúa si la quest tiene estándares NUTRITION como targets
        val today        = timeProvider.today()
        val (start, end) = timeProvider.dayBoundaries(today)
        val nutritionTargets = standardEntryDao.getForDay(uID, start, end)
            .filter { it.standardId in targetIds && it.standardType == "NUTRITION" }

        if (nutritionTargets.isEmpty()) return

        // Score ≥ 0.7 cuenta como progreso para este día
        if (alignmentScore >= 0.7f) {
            val newProgress = quest.currentProgress + 1
            questDao.updateProgress(quest.id, newProgress)
            Timber.tag(TAG).d("Quest general nutrition progress → $newProgress/${quest.goal}")

            if (newProgress >= quest.goal) {
                questDao.updateStatus(quest.id, GeneratedQuestStatus.COMPLETED.name,
                    System.currentTimeMillis())
                awardQuestXP(uID, quest.xpReward)
            }
        }
    }

    suspend fun createPunishmentQuest(uID: String, failedDays: Int) {
        try {
            val existing = questDao.getActive(uID)
            if (existing != null) {
                Timber.tag(TAG).d("Quest de castigo solicitada pero ya hay una activa — skipping")
                return
            }

            val today = timeProvider.today()
            val periodStart = today.minusDays(failedDays.toLong())
            val (startMs, _) = timeProvider.dayBoundaries(periodStart)
            val (_, endMs) = timeProvider.dayBoundaries(today.minusDays(1))
            val recentEntries = standardEntryDao.getForDay(uID, startMs, endMs)
            val failedStandardIds = recentEntries
                .filter { !it.isCompleted }
                .groupBy { it.standardId }
                .entries
                .sortedByDescending { it.value.size }
                .take(2)
                .map { it.key }

            if (failedStandardIds.isEmpty()) {
                Timber.tag(TAG).w("createPunishmentQuest: no hay estándares fallados recientes")
                return
            }

            val failedTitles = recentEntries
                .filter { it.standardId in failedStandardIds }
                .distinctBy { it.standardId }
                .joinToString(", ") { it.standardTitle }

            val startDate = Date()
            val endDate = Date(startDate.time + 5 * 24 * 60 * 60 * 1000L)

            questDao.insert(
                GeneratedQuestEntity(
                    id = UUID.randomUUID().toString(),
                    uID = uID,
                    weeklyReportId = "punishment_${System.currentTimeMillis()}",
                    title = "Recuperación de identidad",
                    description = "Fallaste $failedDays días seguidos en: $failedTitles. Completa estos estándares 5 días consecutivos para recuperar tu racha.",
                    type = GeneratedQuestType.STREAK.name,
                    targetStandardIds = gson.toJson(failedStandardIds),
                    goal = 5,
                    durationDays = 5,
                    currentProgress = 0,
                    status = GeneratedQuestStatus.ACTIVE.name,
                    startDate = startDate,
                    endDate = endDate,
                    xpReward = 200,
                    isSynced = false
                )
            )

            Timber.tag(TAG).i(
                "⚠️ Quest de castigo creada → '$failedTitles' | " +
                        "$failedDays días fallados | 5 días STREAK | +200 XP"
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "createPunishmentQuest() falló")
        }
    }

    private suspend fun awardQuestXP(uID: String, xp: Int) {
        val progress = playerProgressDao.getPlayerProgress(uID) ?: return
        val newXP    = (progress.exp ?: 0) + xp
        val newLevel = LevelCalculator.calculateLevel(newXP)
        playerProgressDao.updatePlayerProgress(progress.copy(exp = newXP, level = newLevel))
    }
}