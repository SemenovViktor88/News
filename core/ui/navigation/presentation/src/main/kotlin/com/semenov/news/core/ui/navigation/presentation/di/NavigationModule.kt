package com.semenov.news.core.ui.navigation.presentation.di

import com.semenov.news.core.ui.navigation.domain.NavigationManager
import com.semenov.news.core.ui.navigation.presentation.NavigationManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {
    @Binds
    @Singleton
    abstract fun bindNavigationManager(implementation: NavigationManagerImpl): NavigationManager
}