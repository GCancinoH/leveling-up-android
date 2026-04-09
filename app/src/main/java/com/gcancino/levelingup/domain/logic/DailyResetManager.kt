package com.gcancino.levelingup.domain.logic

import android.content.SharedPreferences
import androidx.core.content.edit
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
 * DailyResetManager — the deterministic core of the penalty system.
 *
 * KEY PRINCIPLE (from code review):
 * Penalty is NOT triggered by time (WorkManager).
 * It is triggered by STATE INCONSISTENCY detected on app open.
 *
 * This runs on every app start and catches ALL missed days,
 * not just the previous night — so if a user skips 3 days,
 * all 3 days get evaluated.
 *
 * Anti-cheat: user cannot escape by closing the app before midnight.
 * The check is based on date comparison, not execution time.
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
    private val prefs: SharedPreferences  // "penalty_prefs" — injected via Hilt
) {

    private val TAG = "DailyResetManager"
    private val gson = Gson()

    companion object {
        private const val KEY_LAST_EVALUATED_DATE = "last_evaluated_date"
        private const val KEY_LAST_PENALTY_XP     = "last_penalty_xp_lost"
        private const val KEY_LAST_PENALTY_STREAK  = "last_penalty_streak_lost"
        private const val KEY_LAST_PENALTY_COUNT   = "last_penalty_tasks_count"
        private const val KEY_LAST_PENALTY_DATE    = "last_penalty_date"
    }

    /**
     * Call this on every app open — InitViewModel, DashboardViewModel.
     * Evaluates all days from last check until yesterday.
     * Today is never penalized (user still has time).
     *
     * Returns PenaltySummary if penalty was applied, null if everything was clean.
     */
    suspend fun evaluateAndApply(uID: String): PenaltySummary? {
        val today         = timeProvider.today()
        val lastEvaluated = getLastEvaluatedDate()

        Timber.tag(TAG).d("evaluateAndApply() → today: $today | lastEvaluated: $lastEvaluated")

        // Already evaluated today — nothing to do
        if (lastEvaluated != null && lastEvaluated >= today.minusDays(1)) {
            Timber.tag(TAG).d("Already evaluated — skipping")
            return null
        }

        // Evaluate all missed days from (lastEvaluated + 1) to yesterday
        val startDay = lastEvaluated?.plusDays(1) ?: today.minusDays(1)
        val endDay   = today.minusDays(1) // never penalize today

        var totalXpLost    = 0
        var totalStreak    = 0
        var totalIncomplete = 0

        var day = startDay
        while (!day.isAfter(endDay)) {
            val result = evaluateDay(uID, day)
            if (result != null) {
                totalXpLost     += result.xpLost
                totalStreak      = result.streakLost  // streak resets once
                totalIncomplete += result.incompleteTasks
            }
            day = day.plusDays(1)
        }

        // Mark today as evaluated
        markEvaluated(today.minusDays(1))

        if (totalXpLost > 0 || totalIncomplete > 0) {
            val summary = PenaltySummary(
                xpLost          = totalXpLost,
                streakLost      = totalStreak,
                incompleteTasks = totalIncomplete
            )
            savePenaltySummary(summary)
            Timber.tag(TAG).i(
                "✔ Penalties applied → XP: -$totalXpLost | " +
                        "streak reset | incomplete: $totalIncomplete"
            )
            return summary
        }

        return null
    }

    /**
     * Evaluates a specific day.
     * Checks both DailyTasks and IdentityStandards (whichever the user has).
     */
    private suspend fun evaluateDay(uID: String, date: LocalDate): PenaltySummary? {
        val (start, end) = timeProvider.dayBoundaries(date)
        
        generatedQuestManager.evaluateProgress(uID)

        // ── Check identity standards (new system) ─────────────────────────────
        val incompleteStandards = standardEntryDao.getIncompleteForDay(uID, start, end)

        // ── Check daily tasks (legacy) ────────────────────────────────────────
        val incompleteTasks = dailyTaskDao.getIncompletePenaltyEligible(uID, start, end)

        val totalIncomplete = incompleteStandards.size + incompleteTasks.size

        if (totalIncomplete == 0) {
            Timber.tag(TAG).d("$date → clean, no penalty")
            return null
        }

        Timber.tag(TAG).d(
            "$date → $totalIncomplete incomplete " +
                    "(${incompleteStandards.size} standards + ${incompleteTasks.size} tasks)"
        )

        // ── Deduct XP ──────────────────────────────────────────────────────────
        val xpFromStandards = incompleteStandards.sumOf { it.xpAwarded }
        val xpFromTasks     = incompleteTasks.sumOf { it.xpReward }
        val totalXpLost     = xpFromStandards + xpFromTasks

        val progress = playerProgressDao.getPlayerProgress(uID)
        if (progress != null && totalXpLost > 0) {
            val newXP    = maxOf(0, (progress.exp ?: 0) - totalXpLost)
            val newLevel = LevelCalculator.calculateLevel(newXP)
            playerProgressDao.updatePlayerProgress(progress.copy(exp = newXP, level = newLevel))
        }

        // ── Reset streak (only once regardless of how many days missed) ────────
        val streak     = playerStreakDao.getPlayerStreak(uID)
        val streakLost = streak?.currentStreak ?: 0
        if (streak != null && streakLost > 0) {
            playerStreakDao.updateStreak(uID, 0, Date())
        }

        // ── Mark as penalty applied so we never double-penalize ────────────────
        incompleteStandards.forEach { standardEntryDao.markPenaltyApplied(it.id) }
        incompleteTasks.forEach    { dailyTaskDao.markPenaltyApplied(it.id) }

        // ── Record PenaltyEvent ────────────────────────────────────────────────
        val allIncompleteIds = incompleteStandards.map { it.id } + incompleteTasks.map { it.id }
        penaltyEventDao.insert(
            PenaltyEventEntity(
                id              = UUID.randomUUID().toString(),
                uID             = uID,
                date            = Date(start),
                xpLost          = totalXpLost,
                streakLost      = streakLost,
                incompleteTasks = gson.toJson(allIncompleteIds),
                isSynced        = false
            )
        )

        return PenaltySummary(
            xpLost          = totalXpLost,
            streakLost      = streakLost,
            incompleteTasks = totalIncomplete
        )
    }

    // ── Persistence helpers ───────────────────────────────────────────────────

    private fun getLastEvaluatedDate(): LocalDate? {
        val stored = prefs.getString(KEY_LAST_EVALUATED_DATE, null) ?: return null
        return try { LocalDate.parse(stored) } catch (e: Exception) { null }
    }

    private fun markEvaluated(date: LocalDate) {
        prefs.edit { putString(KEY_LAST_EVALUATED_DATE, date.toString()) }
    }

    private fun savePenaltySummary(summary: PenaltySummary) {
        prefs.edit {
            putInt(KEY_LAST_PENALTY_XP,     summary.xpLost)
            putInt(KEY_LAST_PENALTY_STREAK,  summary.streakLost)
            putInt(KEY_LAST_PENALTY_COUNT,   summary.incompleteTasks)
            putLong(KEY_LAST_PENALTY_DATE,   System.currentTimeMillis())
        }
    }
}