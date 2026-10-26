package com.semenov.news.features.auth.data.di

import com.semenov.news.features.auth.data.repository.MockAuthRepository
import com.semenov.news.features.auth.data.session.DatabaseSessionDataCleaner
import com.semenov.news.features.auth.data.session.SessionDataCleaner
import com.semenov.news.features.auth.data.session.SessionManagerImpl
import com.semenov.news.features.auth.domain.repository.AuthRepository
import com.semenov.news.features.auth.domain.session.SessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {
    @Binds
    abstract fun bindAuthRepository(implementation: MockAuthRepository): AuthRepository

    @Binds
    abstract fun bindSessionManager(implementation: SessionManagerImpl): SessionManager

    @Binds
    abstract fun bindSessionDataCleaner(implementation: DatabaseSessionDataCleaner): SessionDataCleaner
}
