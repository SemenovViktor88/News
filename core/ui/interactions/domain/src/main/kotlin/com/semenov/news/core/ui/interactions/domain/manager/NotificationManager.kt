package com.semenov.news.core.ui.interactions.domain.manager

import com.semenov.news.core.ui.interactions.domain.model.notification.AppNotification

interface NotificationManager {
    /** Returns false when notification permission or its channel is disabled. */
    fun showBlocking(notification: AppNotification): Boolean

    suspend fun show(notification: AppNotification): Boolean

    fun dismissBlocking(id: Int)

    suspend fun dismiss(id: Int)

    fun dismissAllBlocking()

    suspend fun dismissAll()
}
