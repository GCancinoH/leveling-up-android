package com.gcancino.levelingup.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.repositories.ExerciseRepositoryImpl
import com.gcancino.levelingup.domain.repositories.ExerciseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*

@HiltWorker
class WeeklySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ExerciseRepositoryImpl
) : CoroutineWorker(context, params) {

    private val TAG = "WeeklySyncWorker"

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("WeeklySyncWorker started ───────────────────────")

        val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        Timber.tag(TAG).d("Targeitng next week starting: $nextMonday")

        return when (val result = repository.syncWeek(nextMonday)) {
            is Resource.Success -> {
                Timber.tag(TAG).i("✔ Weekly sync successful → week of $nextMonday")
                Result.success()
            }
            is Resource.Error -> {
                Timber.tag(TAG).e("✘ Weekly sync failed: ${result.message}. Scheduling retry...")
                Result.retry()
            }
            else -> {
                Timber.tag(TAG).w("Unexpected state during sync. Retrying...")
                Result.retry()
            }
        }
    }
}