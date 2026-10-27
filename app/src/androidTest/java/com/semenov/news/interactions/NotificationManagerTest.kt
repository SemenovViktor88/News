package com.semenov.news.interactions

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.os.Build
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.semenov.news.core.ui.interactions.domain.manager.NotificationManager
import com.semenov.news.core.ui.interactions.domain.model.notification.AppNotification
import com.semenov.news.core.ui.interactions.domain.model.notification.AppNotificationChannel
import com.semenov.news.core.ui.interactions.domain.model.notification.NotificationImportance
import com.semenov.news.core.ui.interactions.domain.model.notification.NotificationVisibility
import dagger.hilt.EntryPoints
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.app.NotificationManager as AndroidNotificationManager

/** The shared singleton must deliver through Android without an Activity or a Compose handler. */
@RunWith(AndroidJUnit4::class)
class NotificationManagerTest {
    private lateinit var manager: NotificationManager
    private lateinit var androidManager: AndroidNotificationManager
    private val channelIds = mutableListOf<String>()

    @Before
    fun allowNotificationsAndResolveSharedManager() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        androidManager = context.getSystemService(AndroidNotificationManager::class.java)
        val entryPoint = EntryPoints.get(
            context.applicationContext,
            NotificationTestDependencies::class.java,
        )
        manager = entryPoint.notificationManager()
        assertSame(manager, entryPoint.notificationManager())
        TEST_NOTIFICATION_IDS.forEach(androidManager::cancel)
    }

    @After
    fun removeTestNotificationsAndChannels() {
        TEST_NOTIFICATION_IDS.forEach(androidManager::cancel)
        channelIds.forEach(androidManager::deleteNotificationChannel)
    }

    @Test
    fun postsNativeContentChannelAndPresentationOptionsWithoutAnActivity() {
        val channel = newChannel(importance = NotificationImportance.High)
        val notification = AppNotification(
            id = FIRST_NOTIFICATION_ID,
            channel = channel,
            title = "Reading reminder",
            message = "Your saved articles are ready.",
            timeoutMillis = 60_000L,
            visibility = NotificationVisibility.Secret,
            autoCancel = false,
        )

        assertTrue(manager.showBlocking(notification))

        val delivered = waitForNotification(FIRST_NOTIFICATION_ID)
        assertEquals(channel.id, delivered.channelId)
        assertEquals(notification.title, delivered.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
        assertEquals(notification.message, delivered.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
        assertEquals(Notification.VISIBILITY_SECRET, delivered.visibility)
        assertEquals(60_000L, delivered.timeoutAfter)
        assertFalse(delivered.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertNotNull("Android needs a small notification icon", delivered.smallIcon)
        val nativeChannel = androidManager.getNotificationChannel(channel.id)
        assertEquals(channel.name, nativeChannel.name.toString())
        assertEquals(channel.description, nativeChannel.description)
        assertEquals(AndroidNotificationManager.IMPORTANCE_HIGH, nativeChannel.importance)

        val defaultNotification = AppNotification(
            id = SECOND_NOTIFICATION_ID,
            channel = channel,
            title = "Daily perspective",
            message = "Your reading summary is ready.",
        )
        assertTrue(runBlocking { manager.show(defaultNotification) })
        val deliveredWithDefaults = waitForNotification(SECOND_NOTIFICATION_ID)
        assertEquals(Notification.VISIBILITY_PRIVATE, deliveredWithDefaults.visibility)
        // Android controls expiration when the app does not request a custom timeout.
        assertTrue(deliveredWithDefaults.flags and Notification.FLAG_AUTO_CANCEL != 0)
    }

    @Test
    fun replacesTheSameIdAndCancelsOnlyRequestedNotifications() {
        val channel = newChannel()
        val first = AppNotification(
            id = FIRST_NOTIFICATION_ID,
            channel = channel,
            title = "First edition",
            message = "The first notification.",
            visibility = NotificationVisibility.Public,
        )
        assertTrue(manager.showBlocking(first))
        assertEquals(Notification.VISIBILITY_PUBLIC, waitForNotification(FIRST_NOTIFICATION_ID).visibility)

        assertTrue(runBlocking { manager.show(first.copy(title = "Updated edition")) })
        waitUntil("The same notification ID must replace its previous content") {
            activeNotifications().singleOrNull { it.id == FIRST_NOTIFICATION_ID }
                ?.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() == "Updated edition"
        }
        val second = first.copy(id = SECOND_NOTIFICATION_ID, title = "Independent reminder")
        assertTrue(runBlocking { manager.show(second) })
        waitForNotification(SECOND_NOTIFICATION_ID)
        assertEquals(2, activeNotifications().count { it.id in TEST_NOTIFICATION_IDS })

        manager.dismissBlocking(FIRST_NOTIFICATION_ID)
        waitUntil("Targeted dismissal must remove the requested notification") {
            activeNotifications().none { it.id == FIRST_NOTIFICATION_ID }
        }
        assertNotNull(activeNotifications().singleOrNull { it.id == SECOND_NOTIFICATION_ID })

        assertTrue(manager.showBlocking(first))
        waitForNotification(FIRST_NOTIFICATION_ID)
        runBlocking { manager.dismiss(SECOND_NOTIFICATION_ID) }
        waitUntil("Suspending dismissal must remove the requested notification") {
            activeNotifications().none { it.id == SECOND_NOTIFICATION_ID }
        }
        assertNotNull(activeNotifications().singleOrNull { it.id == FIRST_NOTIFICATION_ID })

        manager.dismissAllBlocking()
        waitUntil("Blocking dismissal must clear the app's notifications") { activeNotifications().isEmpty() }
        assertTrue(runBlocking { manager.show(first) })
        waitForNotification(FIRST_NOTIFICATION_ID)
        runBlocking { manager.dismissAll() }
        waitUntil("Suspending dismissal must clear the app's notifications") { activeNotifications().isEmpty() }
    }

    @Test
    fun reportsBlockedChannelWithoutPublishingOrOverridingTheUsersChoice() {
        val channel = newChannel(importance = NotificationImportance.High)
        androidManager.createNotificationChannel(
            NotificationChannel(channel.id, channel.name, AndroidNotificationManager.IMPORTANCE_NONE),
        )

        val delivered = manager.showBlocking(
            AppNotification(
                id = FIRST_NOTIFICATION_ID,
                channel = channel,
                title = "Blocked reminder",
                message = "This channel is disabled.",
            ),
        )

        assertFalse("A disabled channel must be reported to the caller", delivered)
        assertEquals(
            AndroidNotificationManager.IMPORTANCE_NONE,
            androidManager.getNotificationChannel(channel.id).importance,
        )
        assertTrue(activeNotifications().none { it.id == FIRST_NOTIFICATION_ID })
    }

    private fun newChannel(
        importance: NotificationImportance = NotificationImportance.Default,
    ): AppNotificationChannel = AppNotificationChannel(
        id = "notification_manager_test_${UUID.randomUUID()}",
        name = "Reading reminders",
        description = "Notifications for native manager integration tests",
        importance = importance,
    ).also { channelIds += it.id }

    private fun activeNotifications(): List<StatusBarNotification> = androidManager.activeNotifications.toList()

    private fun waitForNotification(id: Int): Notification {
        waitUntil("Android must publish notification $id") { activeNotifications().any { it.id == id } }
        return activeNotifications().single { it.id == id }.notification
    }

    private fun waitUntil(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 10_000L
        while (!condition() && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(50L)
        }
        assertTrue(message, condition())
    }

    private companion object {
        const val FIRST_NOTIFICATION_ID = 29_101
        const val SECOND_NOTIFICATION_ID = 29_102
        val TEST_NOTIFICATION_IDS = setOf(FIRST_NOTIFICATION_ID, SECOND_NOTIFICATION_ID)
    }
}
