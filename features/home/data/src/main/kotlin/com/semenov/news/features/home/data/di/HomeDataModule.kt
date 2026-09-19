package com.semenov.news.features.home.data.di

import com.semenov.news.core.domain.repository.NewsRepository
import com.semenov.news.features.home.data.repository.NewsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeDataModule {
    @Binds
    @Singleton
    abstract fun bindNewsRepository(implementation: NewsRepositoryImpl): NewsRepository
}
