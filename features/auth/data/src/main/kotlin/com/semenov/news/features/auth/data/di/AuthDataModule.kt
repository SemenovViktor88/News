package com.semenov.news.features.auth.data.di

import com.semenov.news.features.auth.data.repository.LocalAuthRepository
import com.semenov.news.features.auth.data.verification.AuthChallengeClock
import com.semenov.news.features.auth.data.verification.AuthCodeGenerator
import com.semenov.news.features.auth.data.verification.AuthCodeNotifier
import com.semenov.news.features.auth.data.verification.ElapsedRealtimeAuthChallengeClock
import com.semenov.news.features.auth.data.verification.NotificationAuthCodeNotifier
import com.semenov.news.features.auth.data.verification.SecureAuthCodeGenerator
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
    abstract fun bindAuthRepository(implementation: LocalAuthRepository): AuthRepository

    @Binds
    internal abstract fun bindAuthCodeNotifier(implementation: NotificationAuthCodeNotifier): AuthCodeNotifier

    @Binds
    internal abstract fun bindAuthCodeGenerator(implementation: SecureAuthCodeGenerator): AuthCodeGenerator

    @Binds
    internal abstract fun bindAuthChallengeClock(implementation: ElapsedRealtimeAuthChallengeClock): AuthChallengeClock

    @Binds
    abstract fun bindSessionManager(implementation: SessionManagerImpl): SessionManager

    @Binds
    abstract fun bindSessionDataCleaner(implementation: DatabaseSessionDataCleaner): SessionDataCleaner
}
