package com.gcancino.levelingup.core.notifications

import com.gcancino.levelingup.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        message.notification?.let {
            NotificationHelper.showNotification(
                context = this,
                title = it.title ?: "Leveling Up",
                body = it.body ?: "",
                notificationType = "fcm_push"
            )
        }
    }
}
