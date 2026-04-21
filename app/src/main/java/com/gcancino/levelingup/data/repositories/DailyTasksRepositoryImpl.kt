package com.gcancino.levelingup.data.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.PlayerProgressDao
import com.gcancino.levelingup.data.local.database.dao.PlayerStreakDao
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.DailyTaskDao
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.EveningEntryDao
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.MorningEntryDao
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.PenaltyEventDao
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.DailyTaskEntity
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.EveningEntryEntity
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.MorningEntryEntity
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.PenaltyEventEntity
import com.gcancino.levelingup.data.local.database.mappers.toDomain
import com.gcancino.levelingup.data.local.database.mappers.toEntity
import com.gcancino.levelingup.domain.logic.LevelCalculator
import com.gcancino.levelingup.domain.models.dailyTasks.DailyTask
import com.gcancino.levelingup.domain.models.dailyTasks.EveningEntry
import com.gcancino.levelingup.domain.models.dailyTasks.MorningEntry
import com.gcancino.levelingup.domain.models.dailyTasks.PenaltyEvent
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.room.withTransaction
import com.gcancino.levelingup.data.local.database.AppDatabase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class DailyTasksRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val morningDao: MorningEntryDao,
    private val eveningDao: EveningEntryDao,
    private val taskDao: DailyTaskDao,
    private val penaltyDao: PenaltyEventDao,
    private val playerProgressDao: PlayerProgressDao,
    private val playerStreakDao: PlayerStreakDao,
    private val playerRepository: PlayerRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : DailyTasksRepository {

    private val TAG = "DailyRepository"

    /* Daily Boundaries */
    private fun todayBoundaries(): Pair<Long, Long> = dayBoundaries(Date())

    private fun dayBoundaries(date: Date): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return Pair(start, cal.timeInMillis)
    }

    /*
     * Morning
     */

    override suspend fun saveMorningEntry(entry: MorningEntry): Resource<Unit> {
        return try {
            morningDao.insert(entry.toEntity())
            Timber.tag(TAG).i("✔ Morning entry saved → ${entry.answers.size} answers")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "saveMorningEntry() failed")
            Resource.Error("Failed to save morning entry")
        }
    }

    override fun observeMorningCompletedToday(uID: String): Flow<Boolean> {
        val (start, end) = todayBoundaries()
        return morningDao.countForDay(uID, start, end)
            .map { it > 0 }
    }

    override suspend fun getTodaysMorningEntry(uID: String): MorningEntry? {
        val (start, end) = todayBoundaries()
        return morningDao.getForDay(uID, start, end)?.toDomain()
    }

    /*
    * Evening
    */
    override suspend fun saveEveningEntry(entry: EveningEntry): Resource<Unit> {
        return try {
            eveningDao.insert(entry.toEntity())
            Timber.tag(TAG).i("✔ Evening entry saved → ${entry.answers.size} answers")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "saveEveningEntry() failed")
            Resource.Error("Failed to save evening entry")
        }
    }

    override fun observeEveningCompletedToday(uID: String): Flow<Boolean> {
        val (start, end) = todayBoundaries()
        return eveningDao.countForDay(uID, start, end)
            .map { it > 0 }
    }

    /*
    * Tasks
    */

    override suspend fun saveTasks(tasks: List<DailyTask>): Resource<Unit> {
        return try {
            taskDao.insertAll(tasks.map { it.toEntity() })
            Timber.tag(TAG).i("✔ ${tasks.size} task(s) saved to Room")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "saveTasks() failed")
            Resource.Error("Failed to save tasks")
        }
    }

    override fun getTodaysTasks(uID: String): Flow<List<DailyTask>> {
        val (start, end) = todayBoundaries()
        return taskDao.getTasksForDay(uID, start, end).map { it.map { e -> e.toDomain() } }
    }

    override fun observeTasksForDate(uID: String, date: Date): Flow<List<DailyTask>> {
        val (start, end) = dayBoundaries(date)
        return taskDao.getTasksForDay(uID, start, end).map { it.map { e -> e.toDomain() } }
    }

    override fun getTodaysPendingTasks(uID: String): Flow<List<DailyTask>> {
        val (start, end) = todayBoundaries()
        return taskDao.getPendingTasksForDay(uID, start, end).map { it.map { e -> e.toDomain() } }
    }

    override suspend fun countTodaysTasks(uID: String): Int {
        val (start, end) = todayBoundaries()
        return taskDao.countTasksForDay(uID, start, end)
    }

    /**
     * Marks a task complete, awards XP to PlayerProgress.
     * Returns the current level after XP award.
     */
    override suspend fun completeTask(taskId: String, uID: String): Resource<Int> {
        return try {
            val (start, end) = todayBoundaries()
            val tasks        = taskDao.getTasksForDayOnce(uID, start, end)
            val task         = tasks.firstOrNull { it.id == taskId }
                ?: return Resource.Error("Task not found")

            if (task.isCompleted) {
                return Resource.Error("Task already completed")
            }

            // Mark complete in Room
            taskDao.markCompleted(taskId, System.currentTimeMillis())
            Timber.tag(TAG).d("Task completed → id: $taskId | XP: ${task.xpReward}")

            // Award XP using PlayerRepository for centralized level management
            // This returns the new level
            val awardResult = playerRepository.awardXP(uID, task.xpReward)
            
            if (awardResult is Resource.Success) {
                Resource.Success(awardResult.data ?: 1)
            } else {
                Resource.Error(awardResult.message ?: "Failed to award XP")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "completeTask() failed")
            Resource.Error("Failed to complete task")
        }
    }

    // ─── Midnight Penalty ────────────────────────────────────────────────────────

    override suspend fun applyMidnightPenalty(uID: String): Resource<PenaltyEvent?> {
        return try {
            val (start, end) = todayBoundaries()
            
            val penaltyEvent = appDatabase.withTransaction {
                val incompleteTasks = taskDao.getTasksForDayOnce(uID, start, end)
                    .filter { !it.isCompleted && !it.penaltyApplied }

                if (incompleteTasks.isEmpty()) {
                    return@withTransaction null
                }

                val totalXPLost = incompleteTasks.sumOf { it.xpReward }

                // Deduct XP
                playerProgressDao.getPlayerProgress(uID)?.let { progress ->
                    val newXP    = maxOf(0, (progress.exp ?: 0) - totalXPLost)
                    val newLevel = LevelCalculator.calculateLevel(newXP)
                    playerProgressDao.updatePlayerProgress(
                        progress.copy(exp = newXP, level = newLevel)
                    )
                }

                // Reset streak
                val streak     = playerStreakDao.getPlayerStreak(uID)
                val streakLost = streak?.currentStreak ?: 0
                if (streak != null) {
                    playerStreakDao.updateStreak(uID, 0, Date())
                }

                // Mark tasks as penalty applied atomically via Room batch update
                taskDao.markPenaltyAppliedAtomic(uID, start, end)

                // Record PenaltyEvent
                val event = PenaltyEvent(
                    id = UUID.randomUUID().toString(),
                    uID = uID,
                    date = Date(),
                    xpLost = totalXPLost,
                    streakLost = streakLost,
                    incompleteTasks = incompleteTasks.map { it.id },
                    isSynced = false
                )
                
                penaltyDao.insert(
                    PenaltyEventEntity(
                        id = event.id,
                        uID = event.uID,
                        date = event.date,
                        xpLost = event.xpLost,
                        streakLost = event.streakLost,
                        incompleteTasks = Json.encodeToString(event.incompleteTasks),
                        isSynced = false
                    )
                )
                
                event
            }

            if (penaltyEvent == null) {
                Timber.tag(TAG).i("No incomplete tasks → no penalty applied")
                return Resource.Success(null)
            }

            Timber.tag(TAG).i(
                "✔ Penalty applied → XP: -${penaltyEvent.xpLost} | streak reset from ${penaltyEvent.streakLost}"
            )
            Resource.Success(penaltyEvent)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "applyMidnightPenalty() failed")
            Resource.Error("Penalty failed")
        }
    }

    override suspend fun getLatestPenalty(uID: String): PenaltyEvent? {
        return penaltyDao.getLatest(uID)?.let { entity ->
            PenaltyEvent(
                id              = entity.id,
                uID             = entity.uID,
                date            = entity.date,
                xpLost          = entity.xpLost,
                streakLost      = entity.streakLost,
                incompleteTasks = Json.decodeFromString(entity.incompleteTasks),
                isSynced        = entity.isSynced
            )
        }
    }

    // ─── Sync ─────────────────────────────────────────────────────────────────────

    override suspend fun syncUnsynced(): Resource<Unit> {
        return try {
            val morningEntries  = morningDao.getUnsynced()
            val eveningEntries  = eveningDao.getUnsynced()
            val tasks           = taskDao.getUnsynced()
            val penaltyEvents   = penaltyDao.getUnsynced()

            Timber.tag(TAG).d(
                "syncUnsynced() → morning: ${morningEntries.size} | " +
                        "evening: ${eveningEntries.size} | tasks: ${tasks.size} | " +
                        "penalties: ${penaltyEvents.size}"
            )

            if (morningEntries.isEmpty() && eveningEntries.isEmpty() &&
                tasks.isEmpty() && penaltyEvents.isEmpty()) {
                return Resource.Success(Unit)
            }

            val batch = firestore.batch()

            morningEntries.forEach { entry ->
                val ref = firestore.collection("morning_entries").document(entry.id)
                batch.set(ref, entry.toFirestoreMap())
            }
            eveningEntries.forEach { entry ->
                val ref = firestore.collection("evening_entries").document(entry.id)
                batch.set(ref, entry.toFirestoreMap())
            }
            tasks.forEach { task ->
                val ref = firestore.collection("daily_tasks").document(task.id)
                batch.set(ref, task.toFirestoreMap())
            }
            penaltyEvents.forEach { penalty ->
                val ref = firestore.collection("penalty_events").document(penalty.id)
                batch.set(ref, penalty.toFirestoreMap())
            }

            batch.commit().await()

            // Mark as synced in Room
            morningDao.markAsSynced(morningEntries.map { it.id })
            eveningDao.markAsSynced(eveningEntries.map { it.id })
            taskDao.markAsSynced(tasks.map { it.id })
            penaltyDao.markAsSynced(penaltyEvents.map { it.id })

            Timber.tag(TAG).i("✔ Sync successful")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "syncUnsynced() failed")
            Resource.Error("Sync failed")
        }
    }

    private fun MorningEntryEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "uID" to uID,
        "date" to date,
        "answers" to answers,
        "isSynced" to true
    )

    private fun EveningEntryEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "uID" to uID,
        "date" to date,
        "answers" to answers,
        "isSynced" to true
    )

    private fun DailyTaskEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "uID" to uID,
        "date" to date,
        "title" to title,
        "priority" to priority,
        "isCompleted" to isCompleted,
        "completedAt" to completedAt,
        "xpReward" to xpReward,
        "penaltyApplied" to penaltyApplied,
        "isSynced" to true
    )

    private fun PenaltyEventEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "uID" to uID,
        "date" to date,
        "xpLost" to xpLost,
        "streakLost" to streakLost,
        "incompleteTasks" to incompleteTasks,
        "isSynced" to true
    )
}
