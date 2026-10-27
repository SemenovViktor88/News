package com.semenov.news.auth

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry

/** Exercises delivery through Android notifications instead of sharing the generated code with tests. */
internal fun prepareVerificationNotifications() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }
    verificationNotificationManager().cancel(VERIFICATION_NOTIFICATION_ID)
}

internal fun ComposeTestRule.waitForVerificationCode(previousCode: String? = null): String {
    var deliveredCode: String? = null
    waitUntil(timeoutMillis = 10_000) {
        deliveredCode = verificationNotification()
            ?.extras
            ?.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            ?.let { CODE_PATTERN.find(it)?.value }
        deliveredCode != null && deliveredCode != previousCode
    }
    return requireNotNull(deliveredCode) { "The verification notification must contain a four-digit code" }
}

internal fun verificationNotification(): Notification? =
    verificationNotificationManager().activeNotifications
        .firstOrNull {
            it.id == VERIFICATION_NOTIFICATION_ID && it.notification.channelId == VERIFICATION_CHANNEL_ID
        }
        ?.notification

private fun verificationNotificationManager(): NotificationManager =
    InstrumentationRegistry.getInstrumentation().targetContext
        .getSystemService(NotificationManager::class.java)

private const val VERIFICATION_CHANNEL_ID = "auth_verification"
private const val VERIFICATION_NOTIFICATION_ID = 1001
private val CODE_PATTERN = Regex("\\b[0-9]{4}\\b")
