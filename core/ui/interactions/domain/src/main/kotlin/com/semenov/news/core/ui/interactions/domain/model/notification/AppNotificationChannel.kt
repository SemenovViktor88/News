package com.semenov.news.core.ui.interactions.domain.model.notification

data class AppNotificationChannel(
    val id: String,
    val name: String,
    val description: String? = null,
    val importance: NotificationImportance = NotificationImportance.Default,
) {
    init {
        require(id.isNotBlank()) { "Notification channel ID must not be blank." }
        require(name.isNotBlank()) { "Notification channel name must not be blank." }
    }
}
