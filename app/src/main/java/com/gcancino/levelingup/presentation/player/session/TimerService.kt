package com.gcancino.levelingup.presentation.player.session

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gcancino.levelingup.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.material3.ExperimentalMaterial3Api
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import timber.log.Timber

@ExperimentalAnimationApi
@ExperimentalFoundationApi
class TimerService : Service() {

    // ── #14 fix: SupervisorJob so child failures don't cancel the whole scope ────
    // Dispatchers.Default instead of Main — timer loop must NOT run on main thread
    // or the system will kill the process after ~15-20 seconds under load.
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    // ── #2 fix: proper Binder so ViewModel connects via ServiceConnection ─────────
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = TimerBinder()

    // Exposed to bound clients (ViewModel)
    val timerValue = MutableStateFlow(0)
    val totalSeconds = MutableStateFlow(0) // needed for progress calculation in phase 4

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1
        const val FINISHED_NOTIFICATION_ID = 2
        const val EXTRA_SECONDS = "SECONDS"

        fun startService(context: Context, seconds: Int) {
            Timber.tag("TimerService").d("startService() → $seconds seconds")
            val intent = Intent(context, TimerService::class.java).apply {
                putExtra(EXTRA_SECONDS, seconds)
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            Timber.tag("TimerService").d("stopService()")
            context.stopService(Intent(context, TimerService::class.java))
        }
    }

    // ── #2 fix: return binder so ViewModel can bind and observe timerValue ────────
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getIntExtra(EXTRA_SECONDS, 0) ?: 0
        Timber.tag("TimerService").d("onStartCommand() → $seconds seconds")

        createNotificationChannel()

        // ── #4 fix: use FOREGROUND_SERVICE_TYPE_SPECIAL_USE (correct for timers) ──
        // DATA_SYNC was semantically wrong and risks Play Store rejection.
        // For API < 29 no type is needed.
        // FOREGROUND_SERVICE_TYPE_SPECIAL_USE and DATA_SYNC both require explicit
        // manifest permissions that cause the service to silently fail to start,
        // leading to the "did not call startForeground() in time" ANR.
        // Using the no-type overload works for all API levels for a simple timer.
        try {
            startForeground(NOTIFICATION_ID, createNotification(seconds))
        } catch (e: Exception) {
            Timber.tag("TimerService").e(e, "Error starting foreground service")
        }

        startTimer(seconds)
        return START_NOT_STICKY
    }

    fun startTimer(seconds: Int) {
        timerJob?.cancel()
        totalSeconds.value = seconds
        timerValue.value = seconds
        Timber.tag("TimerService").d("Timer started → $seconds seconds")

        timerJob = serviceScope.launch {
            while (timerValue.value > 0) {
                delay(1000)
                timerValue.value -= 1
                updateNotification(timerValue.value)
            }
            Timber.tag("TimerService").d("Timer finished")
            showTimerFinishedNotification()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerValue.value = 0
        totalSeconds.value = 0
        Timber.tag("TimerService").d("Timer stopped manually")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(seconds: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(seconds))
    }

    private fun createNotification(seconds: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Leveling Up Rest Timer")
            .setContentText("Next set in ${seconds}s")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    private fun showTimerFinishedNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rest Finished!")
            .setContentText("Time is over. Start your next set!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FINISHED_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Workout Timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows the remaining rest time during your workout"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            enableLights(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        Timber.tag("TimerService").d("onDestroy()")
        timerJob?.cancel()
        serviceScope.cancel()
        timerValue.value = 0
        totalSeconds.value = 0
        super.onDestroy()
    }
}
/*class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1
        const val FINISHED_NOTIFICATION_ID = 2
        val timerValue = MutableStateFlow(0)
        
        fun startService(context: Context, seconds: Int) {
            Timber.tag("TimerService").d("Starting service with $seconds seconds")
            val intent = Intent(context, TimerService::class.java).apply {
                putExtra("SECONDS", seconds)
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            Timber.tag("TimerService").d("Stopping service")
            val intent = Intent(context, TimerService::class.java)
            context.stopService(intent)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getIntExtra("SECONDS", 0) ?: 0
        Timber.tag("TimerService").d("onStartCommand: $seconds")
        
        createNotificationChannel()
        
        val notification = createNotification(seconds)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Timber.tag("TimerService").e(e, "Error starting foreground service")
        }
        
        startTimer(seconds)
        
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        timerValue.value = seconds
        timerJob = serviceScope.launch {
            while (timerValue.value > 0) {
                delay(1000)
                timerValue.value -= 1
                updateNotification(timerValue.value)
            }
            Timber.tag("TimerService").d("Timer finished")
            showTimerFinishedNotification()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun updateNotification(seconds: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(seconds))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun createNotification(seconds: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Leveling Up Rest Timer")
            .setContentText("Next set in ${seconds}s")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun showTimerFinishedNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rest Finished!")
            .setContentText("Time is over. Start your next set!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FINISHED_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Workout Timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows the remaining rest time during your workout"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            enableLights(true)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.tag("TimerService").d("onDestroy")
        timerJob?.cancel()
        serviceScope.cancel()
        timerValue.value = 0
        super.onDestroy()
    }
}*/
