package com.semenov.news.interactions

import com.semenov.news.core.ui.interactions.domain.manager.NotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Access to the shared manager for native notification tests; excluded from release builds. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationTestDependencies {
    fun notificationManager(): NotificationManager
}
