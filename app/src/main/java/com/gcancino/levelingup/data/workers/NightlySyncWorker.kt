package com.gcancino.levelingup.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.logic.DailyResetManager
import com.gcancino.levelingup.domain.repositories.BodyDataRepository
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.repositories.IdentityRepository
import com.gcancino.levelingup.domain.repositories.NutritionRepository
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import timber.log.Timber

// ─── NightlySyncWorker ────────────────────────────────────────────────────────
// Role: SYNC only — push unsynced Room data to Firestore.
// Also runs DailyResetManager as safety net (in case user never opened app).
// This guarantees data is never lost even if the user doesn't open the app.

@HiltWorker
class NightlySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val bodyDataRepository: BodyDataRepository,
    private val dailyTasksRepository: DailyTasksRepository,
    private val identityRepository: IdentityRepository,
    private val nutritionRepository: NutritionRepository,
    private val playerRepository: PlayerRepository,
    private val dailyResetManager: DailyResetManager
) : CoroutineWorker(context, params) {

    private val TAG = "NightlySyncWorker"

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("NightlySyncWorker started")

        val uid = auth.currentUser?.uid ?: run {
            Timber.tag(TAG).w("No authenticated user — skipping")
            return Result.success()
        }

        return try {
            coroutineScope {
                val resetJob = async {
                    try { dailyResetManager.evaluateAndApply(uid) }
                    catch (e: Exception) { Timber.tag(TAG).w("Reset failed: ${e.message}") }
                }

                val bodySync      = async { bodyDataRepository.syncUnsynced() }
                val dailySync     = async { dailyTasksRepository.syncUnsynced() }
                val identitySync  = async { identityRepository.syncUnsynced() }
                val nutritionSync = async { nutritionRepository.syncUnsynced() }
                val playerSync    = async { playerRepository.syncUnsynced() }  // ← NUEVO

                resetJob.await()
                val results = awaitAll(bodySync, dailySync, identitySync, nutritionSync, playerSync)

                val error = results.filterIsInstance<Resource.Error<*>>().firstOrNull()
                if (error != null) {
                    Timber.tag(TAG).e("Sync failed: ${error.message}")
                    Result.retry()
                } else {
                    Timber.tag(TAG).i("✔ Nightly sync complete")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "NightlySyncWorker failed")
            Result.retry()
        }
    }
}