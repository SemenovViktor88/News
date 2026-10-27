package com.semenov.news.features.auth.data.verification

import android.content.Context
import com.semenov.news.core.ui.interactions.domain.manager.NotificationManager
import com.semenov.news.core.ui.interactions.domain.model.notification.AppNotification
import com.semenov.news.core.ui.interactions.domain.model.notification.AppNotificationChannel
import com.semenov.news.core.ui.interactions.domain.model.notification.NotificationImportance
import com.semenov.news.core.ui.interactions.domain.model.notification.NotificationVisibility
import com.semenov.news.features.auth.data.R
import com.semenov.news.features.auth.data.repository.CODE_VALIDITY_MILLIS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class NotificationAuthCodeNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager,
) : AuthCodeNotifier {
    override fun show(code: String): Boolean = notificationManager.showBlocking(
        AppNotification(
            id = NOTIFICATION_ID,
            channel = AppNotificationChannel(
                id = CHANNEL_ID,
                name = context.getString(R.string.auth_notification_channel_name),
                description = context.getString(R.string.auth_notification_channel_description),
                importance = NotificationImportance.High,
            ),
            title = context.getString(R.string.auth_notification_title),
            message = context.getString(R.string.auth_notification_code, code),
            timeoutMillis = CODE_VALIDITY_MILLIS,
            visibility = NotificationVisibility.Secret,
            autoCancel = false,
        ),
    )

    override fun clear() = notificationManager.dismissBlocking(NOTIFICATION_ID)

    private companion object {
        const val CHANNEL_ID = "auth_verification"
        const val NOTIFICATION_ID = 1001
    }
}
