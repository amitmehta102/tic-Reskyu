package com.reskyu.consumer.data.model

enum class NotificationType { NEW_DROP, ORDER, IMPACT, ALERT, SYSTEM }

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.SYSTEM,
    val deepLink: String? = null
)
