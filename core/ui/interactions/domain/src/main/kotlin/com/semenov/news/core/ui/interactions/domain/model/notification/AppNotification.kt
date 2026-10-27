package com.semenov.news.core.ui.interactions.domain.model.notification

data class AppNotification(
    val id: Int,
    val channel: AppNotificationChannel,
    val title: String,
    val message: String,
    val timeoutMillis: Long? = null,
    val visibility: NotificationVisibility = NotificationVisibility.Private,
    val autoCancel: Boolean = true,
) {
    init {
        require(timeoutMillis == null || timeoutMillis > 0) {
            "Notification timeout must be positive."
        }
    }

    override fun toString(): String =
        "AppNotification(id=$id, channelId=${channel.id}, payload=<redacted>, " +
            "timeoutMillis=$timeoutMillis, visibility=$visibility, autoCancel=$autoCancel)"
}
