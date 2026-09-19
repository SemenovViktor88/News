package com.semenov.news.core.network.data.di

import com.semenov.news.core.domain.AppInfoProvider
import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.network.data.initHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideHttpClient(
        infoProvider: AppInfoProvider,
        logger: AppLogger,
    ): HttpClient = initHttpClient(
        appLogger = logger,
        infoProvider = infoProvider,
    )
}
