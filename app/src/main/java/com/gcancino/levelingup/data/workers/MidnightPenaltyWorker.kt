package com.gcancino.levelingup.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.repositories.DailyTasksRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import androidx.core.content.edit

@HiltWorker
class MidnightPenaltyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dailyRepository: DailyTasksRepositoryImpl,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, params) {

    private val TAG = "MidnightPenaltyWorker"

    // ── Store penalty result in SharedPreferences so morning flow can show summary
    private val prefs = context.getSharedPreferences("penalty_prefs", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("─── MidnightPenaltyWorker started ──────────────────")

        val uID = auth.currentUser?.uid ?: run {
            Timber.tag(TAG).w("No authenticated user — skipping penalty check")
            return Result.success()
        }

        return when (val result = dailyRepository.applyMidnightPenalty(uID)) {
            is Resource.Success -> {
                val penalty = result.data
                if (penalty != null) {
                    // Store penalty summary for morning flow to read
                    prefs.edit {
                        putInt("last_penalty_xp_lost", penalty.xpLost)
                            .putInt("last_penalty_streak_lost", penalty.streakLost)
                            .putInt("last_penalty_tasks_count", penalty.incompleteTasks.size)
                            .putLong("last_penalty_date", penalty.date.time)
                    }

                    Timber.tag(TAG).i(
                        "%skull", "✔ Penalty applied → XP: -${penalty.xpLost} | " +
                                "streak: -${penalty.streakLost} | "
                    )
                } else {
                    Timber.tag(TAG).i("✔ No penalty — all tasks completed")
                }
                Result.success()
            }
            is Resource.Error -> {
                Timber.tag(TAG).e("Penalty worker failed: ${result.message}")
                Result.retry()
            }
            else -> Result.retry()
        }
    }
}