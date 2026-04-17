package com.gcancino.levelingup

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gcancino.levelingup.data.workers.MidnightPenaltyWorker
import com.gcancino.levelingup.data.workers.NightlySyncWorker
import com.gcancino.levelingup.data.workers.WeeklySyncWorker
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class LevelingUpApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Firebase must stay on main thread — it's fast and required before any DI
        FirebaseApp.initializeApp(this)

        Thread {
            scheduleWeeklySync()
            scheduleMidnightPenalty()
            scheduleNightlySync()
        }.apply {
            name     = "WorkManagerInit"
            isDaemon = true
            start()
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
}