package com.semenov.core.logging.di

import com.semenov.core.logging.TimberInitializer
import com.semenov.news.core.domain.AppInitializer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {

    @Binds
    @IntoSet
    abstract fun bindTimberInitializer(
        initializer: TimberInitializer,
    ): AppInitializer
}
