package com.gcancino.levelingup.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.app.NotificationCompat
import com.gcancino.levelingup.MainActivity
import com.gcancino.levelingup.R

object NotificationHelper {

    private const val DEFAULT_CHANNEL_ID = "leveling_up_general"
    private const val DEFAULT_CHANNEL_NAME = "General Notifications"

    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        notificationType: String = "general",
        notificationId: Int = 0
    ) {
        val intent = if (notificationType == "morning_flow" || notificationType == "evening_flow") {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("levelingup://$notificationType"),
                context,
                MainActivity::class.java
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                action = "com.gcancino.levelingup.NOTIFICATION_CLICK"
                putExtra("notification_type", notificationType)
                putExtra("notification_title", title)
                putExtra("notification_body", body)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                DEFAULT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.levelingup_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
