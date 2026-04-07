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
import com.gcancino.levelingup.domain.logic.DailyResetManager

@HiltWorker
class MidnightPenaltyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val dailyResetManager: DailyResetManager
) : CoroutineWorker(context, params) {

    private val TAG = "MidnightPenaltyWorker"

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("MidnightPenaltyWorker started (backup)")

        val uid = auth.currentUser?.uid ?: run {
            Timber.tag(TAG).w("No authenticated user — skipping")
            return Result.success()
        }

        return try {
            // DailyResetManager is idempotent — safe to call multiple times
            val penalty = dailyResetManager.evaluateAndApply(uid)
            Timber.tag(TAG).i(
                if (penalty != null) "✔ Backup penalty applied → -${penalty.xpLost} XP"
                else "✔ No penalty needed (already applied on app open, or nothing to penalize)"
            )
            Result.success()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "MidnightPenaltyWorker failed")
            Result.retry()
        }
    }
}