package com.gcancino.levelingup.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gcancino.levelingup.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FlowNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val flowType = inputData.getString(KEY_FLOW_TYPE) ?: return Result.failure()

        val (title, body) = when (flowType) {
            TYPE_MORNING -> "Morning Flow" to "Start your day with intention. Complete your morning check-in."
            TYPE_EVENING -> "Evening Flow" to "Reflect on your day. Complete your evening check-in."
            else -> return Result.failure()
        }

        NotificationHelper.showNotification(
            context = context,
            title = title,
            body = body,
            notificationType = flowType,
            notificationId = if (flowType == TYPE_MORNING) 1001 else 1002
        )

        return Result.success()
    }

    companion object {
        const val KEY_FLOW_TYPE = "flow_type"
        const val TYPE_MORNING = "morning_flow"
        const val TYPE_EVENING = "evening_flow"
    }
}
