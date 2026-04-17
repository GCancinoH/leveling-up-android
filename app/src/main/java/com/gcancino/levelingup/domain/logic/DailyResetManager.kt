package com.gcancino.levelingup.domain.logic

import com.gcancino.levelingup.data.local.datastore.DataStoreManager
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.DailyStandardEntryDao
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.DailyTaskDao
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.PenaltyEventDao
import com.gcancino.levelingup.data.local.database.dao.PlayerProgressDao
import com.gcancino.levelingup.data.local.database.dao.PlayerStreakDao
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.PenaltyEventEntity
import com.gcancino.levelingup.domain.models.dailyTasks.PenaltySummary
import com.google.gson.Gson
import timber.log.Timber
import java.time.LocalDate
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DailyResetManager — motor determinístico del sistema de identidad.
 *
 * Sistema de streak completo:
 *   Día cumplido → streak++, longestStreak si record, token si milestone
 *   Día fallado con token → streak protegido, token consumido, XP deducido igual
 *   Día fallado sin token → streak = 0, XP deducido
 *   3 días consecutivos fallados → quest de castigo automática
 *
 * Milestones de protección:
 *   7 días → +1 token | 14 días → +1 token | 30 días → +2 tokens
 *   60 días → +2 tokens | 100 días → +3 tokens
 */
@Singleton
class DailyResetManager @Inject constructor(
    private val timeProvider: TimeProvider,
    private val playerProgressDao: PlayerProgressDao,
    private val playerStreakDao: PlayerStreakDao,
    private val dailyTaskDao: DailyTaskDao,
    private val standardEntryDao: DailyStandardEntryDao,
    private val penaltyEventDao: PenaltyEventDao,
    private val generatedQuestManager: GeneratedQuestManager,
    private val dataStoreManager: DataStoreManager
) {

    private val TAG  = "DailyResetManager"
    private val gson = Gson()

    companion object {
        private val STREAK_MILESTONES = mapOf(
            7 to 1, 14 to 1, 30 to 2, 60 to 2, 100 to 3
        )

        private const val PUNISHMENT_THRESHOLD = 3
    }

    suspend fun evaluateAndApply(uID: String): PenaltySummary? {
        val today         = timeProvider.today()
        val lastEvaluated = getLastEvaluatedDate()

        Timber.tag(TAG).d("evaluateAndApply() → today: $today | lastEvaluated: $lastEvaluated")

        if (lastEvaluated != null && lastEvaluated >= today) {
            Timber.tag(TAG).d("Already evaluated — skipping")
            return null
        }

        val startDay = lastEvaluated?.plusDays(1) ?: today.minusDays(1)
        val endDay   = today.minusDays(1)

        var totalXpLost     = 0
        var totalStreak     = 0
        var totalIncomplete = 0

        var day = startDay
        while (!day.isAfter(endDay)) {
            val result = evaluateDay(uID, day)
            if (result != null) {
                totalXpLost     += result.xpLost
                totalStreak      = result.streakLost
                totalIncomplete += result.incompleteTasks
            }
            day = day.plusDays(1)
        }

        markEvaluated(today.minusDays(1))

        if (totalXpLost > 0 || totalIncomplete > 0) {
            val summary = PenaltySummary(
                xpLost          = totalXpLost,
                streakLost      = totalStreak,
                incompleteTasks = totalIncomplete
            )
            dataStoreManager.savePenalty(summary)
            Timber.tag(TAG).i(
                "✔ Penalizaciones aplicadas → XP: -$totalXpLost | " +
                        "streak: $totalStreak | incompletos: $totalIncomplete"
            )
            return summary
        }

        return null
    }

    private suspend fun evaluateDay(uID: String, date: LocalDate): PenaltySummary? {
        val (start, end) = timeProvider.dayBoundaries(date)
        val now          = System.currentTimeMillis()

        generatedQuestManager.evaluateProgress(uID)

        val incompleteStandards = standardEntryDao.getIncompleteForDay(uID, start, end)
        val incompleteTasks     = dailyTaskDao.getIncompletePenaltyEligible(uID, start, end)
        val totalIncomplete     = incompleteStandards.size + incompleteTasks.size

        // ── Día limpio: incrementar racha ─────────────────────────────────────
        if (totalIncomplete == 0) {
            handleCleanDay(uID, now)
            Timber.tag(TAG).d("$date → identidad cumplida ✓")
            return null
        }

        Timber.tag(TAG).d(
            "$date → $totalIncomplete incompleto(s) " +
                    "(${incompleteStandards.size} estándares + ${incompleteTasks.size} tasks)"
        )

        // ── Deducir XP ────────────────────────────────────────────────────────
        val totalXpLost = incompleteStandards.sumOf { it.xpAwarded } +
                incompleteTasks.sumOf { it.xpReward }

        val progress = playerProgressDao.getPlayerProgress(uID)
        if (progress != null && totalXpLost > 0) {
            val newXP    = maxOf(0, (progress.exp ?: 0) - totalXpLost)
            val newLevel = LevelCalculator.calculateLevel(newXP)
            playerProgressDao.updateProgress(uID, progress.availablePoints, newLevel, newXP)
        }

        // ── Gestión de racha ──────────────────────────────────────────────────
        val streak = playerStreakDao.getPlayerStreak(uID)
        val streakLost: Int

        if (streak != null && streak.protectedDays > 0) {
            // Token disponible → proteger racha
            playerStreakDao.consumeProtectionToken(uID)
            streakLost = 0
            Timber.tag(TAG).i(
                "🛡️ Racha protegida → quedan ${streak.protectedDays - 1} tokens"
            )
        } else {
            // Sin tokens → resetear
            streakLost = streak?.currentStreak ?: 0
            playerStreakDao.resetStreak(uID, now)
            Timber.tag(TAG).i("💀 Racha perdida → era $streakLost días")
        }

        // ── Failures consecutivos → quest de castigo ──────────────────────────
        val consecutiveFails = getConsecutiveFailures() + 1
        saveConsecutiveFailures(consecutiveFails)

        if (consecutiveFails >= PUNISHMENT_THRESHOLD) {
            generatedQuestManager.createPunishmentQuest(uID, consecutiveFails)
            saveConsecutiveFailures(0)
        }

        // ── Marcar penalizados ────────────────────────────────────────────────
        incompleteStandards.forEach { standardEntryDao.markPenaltyApplied(it.id) }
        incompleteTasks.forEach    { dailyTaskDao.markPenaltyApplied(it.id) }

        // ── Registrar PenaltyEvent ────────────────────────────────────────────
        val allIds = incompleteStandards.map { it.id } + incompleteTasks.map { it.id }
        penaltyEventDao.insert(
            PenaltyEventEntity(
                id              = UUID.randomUUID().toString(),
                uID             = uID,
                date            = Date(start),
                xpLost          = totalXpLost,
                streakLost      = streakLost,
                incompleteTasks = gson.toJson(allIds),
                isSynced        = false
            )
        )

        return PenaltySummary(
            xpLost          = totalXpLost,
            streakLost      = streakLost,
            incompleteTasks = totalIncomplete
        )
    }

    private suspend fun handleCleanDay(uID: String, now: Long) {
        val streak    = playerStreakDao.getPlayerStreak(uID) ?: return
        val newStreak = streak.currentStreak + 1

        playerStreakDao.incrementStreak(uID, newStreak, now)
        saveConsecutiveFailures(0)

        // Milestone → token de protección
        val tokensToAdd = STREAK_MILESTONES[newStreak]
        if (tokensToAdd != null) {
            playerStreakDao.addProtectionTokens(uID, tokensToAdd)
            Timber.tag(TAG).i(
                "🏆 Milestone $newStreak días → +$tokensToAdd token(s) de protección"
            )
        }

        Timber.tag(TAG).d("Racha: $newStreak días")
    }

    private suspend fun getLastEvaluatedDate(): LocalDate? {
        val stored = dataStoreManager.getLastEvaluatedDate() ?: return null
        return try { LocalDate.parse(stored) } catch (e: Exception) { null }
    }

    private suspend fun markEvaluated(date: LocalDate) {
        dataStoreManager.setLastEvaluatedDate(date.toString())
    }

    private suspend fun getConsecutiveFailures() = dataStoreManager.getConsecutiveFailures()

    private suspend fun saveConsecutiveFailures(count: Int) {
        dataStoreManager.saveConsecutiveFailures(count)
    }
}