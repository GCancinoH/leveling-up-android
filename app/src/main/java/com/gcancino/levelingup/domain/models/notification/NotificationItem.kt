package com.gcancino.levelingup.domain.models.notification

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val action: (() -> Unit)? = null
)
