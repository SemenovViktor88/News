package com.semenov.news.core.ui.interactions.data.di

import com.semenov.news.core.ui.interactions.data.manager.HapticEffectManagerImpl
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class InteractionsModule {
    @Binds
    abstract fun bindHapticEffectManager(implementation: HapticEffectManagerImpl): HapticEffectManager
}
