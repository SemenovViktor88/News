package com.semenov.news.di

import com.semenov.news.core.domain.AppInfoProvider
import com.semenov.news.core.domain.AppLogger
import com.semenov.news.util.AppInfoProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideInfoProvider(): AppInfoProvider = AppInfoProviderImpl()

    @Provides
    @Singleton
    fun provideLogger(): AppLogger = object : AppLogger {
        override fun log(tag: String, message: String, t: Throwable?) {
            t?.let { Timber.tag(tag).e(it, message) } ?: Timber.tag(tag).d(message)
        }
    }
}