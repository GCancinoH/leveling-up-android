package com.gcancino.levelingup.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.repositories.DailyTasksRepositoryImpl
import com.gcancino.levelingup.domain.repositories.BodyDataRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import timber.log.Timber

@HiltWorker
class NightlySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val bodyDataRepository: BodyDataRepository,
    private val dailyTasksRepositoryImpl: DailyTasksRepositoryImpl
) : CoroutineWorker(context, params) {

    private val TAG = "NightlySyncWorker"

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("NightlySyncWorker started!")

        return coroutineScope {
            val bodySync = async { bodyDataRepository.syncUnsynced() }
            val dailyTasksSync = async { dailyTasksRepositoryImpl.syncUnsynced() }

            val bodyResult = bodySync.await()
            val dailyTasksResult = dailyTasksSync.await()

            if (bodyResult is Resource.Error || dailyTasksResult is Resource.Error) {
                Timber.tag(TAG).e(
                    "Sync failed → body: ${(bodyResult as? Resource.Error)?.message} | " +
                            "daily: ${(dailyTasksResult as? Resource.Error)?.message}"
                )
                Result.retry()
            } else {
                Timber.tag(TAG).i("✔ Nightly sync successful")
                Result.success()
            }
        }

        /*return when (val result = bodyDataRepository.syncUnsynced()) {
            is Resource.Success -> {
                Timber.tag(TAG).i("✔ Nightly sync successful")
                Result.success()
            }
            is Resource.Error -> {
                Timber.tag(TAG).e("✘ Nightly sync failed: ${result.message}. Retrying...")
                Result.retry()
            }
            else -> Result.retry()
        }*/

    }
}