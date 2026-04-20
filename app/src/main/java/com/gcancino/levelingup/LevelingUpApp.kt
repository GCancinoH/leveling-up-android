package com.gcancino.levelingup

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gcancino.levelingup.data.workers.FlowNotificationWorker
import com.gcancino.levelingup.data.workers.MidnightPenaltyWorker
import com.gcancino.levelingup.data.workers.NightlySyncWorker
import com.gcancino.levelingup.data.workers.WeeklySyncWorker
import com.gcancino.levelingup.utils.AssetsUtil
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.work.workDataOf

@HiltAndroidApp
class LevelingUpApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Firebase must stay on main thread — it's fast and required before any DI
        FirebaseApp.initializeApp(this)

        applicationScope.launch(Dispatchers.IO) {
            // Unpack Vosk model in background to avoid ANR on first start
            AssetsUtil.unpackAssetsFolder(this@LevelingUpApp, "model-en-us")
            
            // Modernized WorkManager initialization
            scheduleWeeklySync()
            scheduleMidnightPenalty()
            scheduleNightlySync()
            scheduleFlowReminders()
        }
    }

    private fun scheduleWeeklySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<WeeklySyncWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(calculateDelayUntilSunday8PM(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeeklySync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun scheduleNightlySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Runs once per day — initial delay targets 2 AM tonight
        val syncRequest = PeriodicWorkRequestBuilder<NightlySyncWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateDelayUntil2AM(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NightlySync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun scheduleMidnightPenalty() {
        val penaltyRequest = PeriodicWorkRequestBuilder<MidnightPenaltyWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateDelayUntilMidnight(), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MidnightPenalty",
            ExistingPeriodicWorkPolicy.KEEP,
            penaltyRequest
        )
    }

    private fun calculateDelayUntil2AM(): Long {
        val now    = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Already past 2 AM today → target tomorrow 2 AM
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delayMs = target.timeInMillis - now.timeInMillis
        Timber.tag("LevelingUpApp").d("NightlySync scheduled → next run in ${delayMs / 1000 / 60} minutes")
        return delayMs
    }

    private fun calculateDelayUntilSunday8PM(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.WEEK_OF_YEAR, 1)
        }
        val delayMs = target.timeInMillis - now.timeInMillis
        Timber.tag("LevelingUpApp").d("WeeklySync scheduled → next run in ${delayMs / 1000 / 60} minutes (${target.time})")
        return delayMs
    }


    private fun calculateDelayUntilMidnight(): Long {
        val now    = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1) // always next midnight
        }
        val delayMs = target.timeInMillis - now.timeInMillis
        Timber.tag("LevelingUpApp").d(
            "MidnightPenalty scheduled → next run in ${delayMs / 1000 / 60} minutes"
        )
        return delayMs
    }

    private fun scheduleFlowReminders() {
        val workManager = WorkManager.getInstance(this)

        // Morning Reminder (8:00 AM)
        val morningRequest = PeriodicWorkRequestBuilder<FlowNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateDelayUntil(8, 0), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(FlowNotificationWorker.KEY_FLOW_TYPE to FlowNotificationWorker.TYPE_MORNING))
            .build()

        workManager.enqueueUniquePeriodicWork(
            "MorningFlowReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            morningRequest
        )

        // Evening Reminder (8:00 PM)
        val eveningRequest = PeriodicWorkRequestBuilder<FlowNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateDelayUntil(20, 0), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(FlowNotificationWorker.KEY_FLOW_TYPE to FlowNotificationWorker.TYPE_EVENING))
            .build()

        workManager.enqueueUniquePeriodicWork(
            "EveningFlowReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            eveningRequest
        )
    }

    private fun calculateDelayUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}