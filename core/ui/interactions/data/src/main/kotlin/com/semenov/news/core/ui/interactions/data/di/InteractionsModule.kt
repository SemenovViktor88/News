package com.semenov.news.core.ui.interactions.data.di

import com.semenov.news.core.ui.interactions.data.manager.DialogManagerImpl
import com.semenov.news.core.ui.interactions.data.manager.HapticEffectManagerImpl
import com.semenov.news.core.ui.interactions.data.manager.ModalManagerImpl
import com.semenov.news.core.ui.interactions.data.manager.NotificationManagerImpl
import com.semenov.news.core.ui.interactions.domain.manager.DialogManager
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import com.semenov.news.core.ui.interactions.domain.manager.ModalManager
import com.semenov.news.core.ui.interactions.domain.manager.NotificationManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class InteractionsModule {
    @Binds
    abstract fun bindNotificationManager(implementation: NotificationManagerImpl): NotificationManager

    @Binds
    abstract fun bindModalManager(implementation: ModalManagerImpl): ModalManager

    @Binds
    abstract fun bindDialogManager(implementation: DialogManagerImpl): DialogManager

    @Binds
    abstract fun bindHapticEffectManager(implementation: HapticEffectManagerImpl): HapticEffectManager
}
