package com.semenov.news.features.news.data.di

import com.semenov.news.features.news.data.datasource.NewsLocalDataSourseImpl
import com.semenov.news.features.news.data.datasource.NewsRemoteDataSourseImpl
import com.semenov.news.features.news.data.repository.NewsRepositoryImpl
import com.semenov.news.features.news.domain.datasource.NewsLocalDataSourse
import com.semenov.news.features.news.domain.datasource.NewsRemoteDataSourse
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

    @Binds
    @Singleton
    abstract fun bindNewsLocalDataSourse(
        implementation: NewsLocalDataSourseImpl,
    ): NewsLocalDataSourse

    @Binds
    @Singleton
    abstract fun bindNewsRemoteDataSourse(
        implementation: NewsRemoteDataSourseImpl,
    ): NewsRemoteDataSourse
}
