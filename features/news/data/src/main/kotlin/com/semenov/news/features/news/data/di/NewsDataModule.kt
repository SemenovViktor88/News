package com.semenov.news.features.news.data.di

import com.semenov.news.features.news.data.repository.NewsRepositoryImpl
import com.semenov.news.features.news.domain.repository.NewsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NewsDataModule {
    @Binds
    @Singleton
    abstract fun bindNewsRepository(implementation: NewsRepositoryImpl): NewsRepository
}
