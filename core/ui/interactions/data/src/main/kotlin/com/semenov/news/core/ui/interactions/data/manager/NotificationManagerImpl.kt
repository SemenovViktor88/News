package com.semenov.news.core.ui.interactions.data.manager

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.designsystem.R as DesignSystemR
import com.semenov.news.core.ui.interactions.domain.manager.NotificationManager
import com.semenov.news.core.ui.interactions.domain.model.notification.AppNotification
import com.semenov.news.core.ui.interactions.domain.model.notification.NotificationImportance
import com.semenov.news.core.ui.interactions.domain.model.notification.NotificationVisibility
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class NotificationManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logger: AppLogger,
) : NotificationManager {
    private val manager = context.getSystemService(AndroidNotificationManager::class.java)

    override fun showBlocking(notification: AppNotification): Boolean = try {
        manager.createNotificationChannel(
            NotificationChannel(
                notification.channel.id,
                notification.channel.name,
                notification.channel.importance.asAndroidImportance,
            ).apply {
                description = notification.channel.description
                lockscreenVisibility = notification.visibility.asAndroidVisibility
            },
        )

        if (canNotify(notification.channel.id)) {
            val builder = NotificationCompat.Builder(context, notification.channel.id)
                .setSmallIcon(DesignSystemR.drawable.ic_notification_news)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
                .setPriority(notification.channel.importance.asCompatPriority)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(notification.visibility.asAndroidVisibility)
                .setAutoCancel(notification.autoCancel)
                .setOnlyAlertOnce(false)

            notification.timeoutMillis?.let(builder::setTimeoutAfter)
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launchIntent ->
                builder.setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        notification.id,
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }

            manager.notify(notification.id, builder.build())
            logger.log(tag = TAG, message = "Show notification: ${notification.id}")
            true
        } else {
            logger.log(tag = TAG, message = "Notification disabled: ${notification.id}")
            false
        }
    } catch (_: SecurityException) {
        // Permission may be revoked between the check and the system call.
        logger.log(tag = TAG, message = "Notification permission denied: ${notification.id}")
        false
    }

    override suspend fun show(notification: AppNotification): Boolean = withContext(Dispatchers.IO) {
        showBlocking(notification)
    }

    override fun dismissBlocking(id: Int) {
        manager.cancel(id)
        logger.log(tag = TAG, message = "Dismiss notification: $id")
    }

    override suspend fun dismiss(id: Int) = withContext(Dispatchers.IO) {
        dismissBlocking(id)
    }

    override fun dismissAllBlocking() {
        manager.cancelAll()
        logger.log(tag = TAG, message = "Dismiss all notifications")
    }

    override suspend fun dismissAll() = withContext(Dispatchers.IO) {
        dismissAllBlocking()
    }

    private fun canNotify(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return manager.getNotificationChannel(channelId)?.importance != AndroidNotificationManager.IMPORTANCE_NONE
    }

    private companion object {
        const val TAG = "NotificationManager"
    }
}

private val NotificationImportance.asAndroidImportance: Int
    get() = when (this) {
        NotificationImportance.Low -> AndroidNotificationManager.IMPORTANCE_LOW
        NotificationImportance.Default -> AndroidNotificationManager.IMPORTANCE_DEFAULT
        NotificationImportance.High -> AndroidNotificationManager.IMPORTANCE_HIGH
    }

private val NotificationImportance.asCompatPriority: Int
    get() = when (this) {
        NotificationImportance.Low -> NotificationCompat.PRIORITY_LOW
        NotificationImportance.Default -> NotificationCompat.PRIORITY_DEFAULT
        NotificationImportance.High -> NotificationCompat.PRIORITY_HIGH
    }

private val NotificationVisibility.asAndroidVisibility: Int
    get() = when (this) {
        NotificationVisibility.Public -> NotificationCompat.VISIBILITY_PUBLIC
        NotificationVisibility.Private -> NotificationCompat.VISIBILITY_PRIVATE
        NotificationVisibility.Secret -> NotificationCompat.VISIBILITY_SECRET
    }
