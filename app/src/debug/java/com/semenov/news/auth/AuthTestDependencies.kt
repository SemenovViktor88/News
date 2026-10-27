package com.semenov.news.auth

import com.semenov.news.core.db.NewsDatabase
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.storage.domain.Storage
import com.semenov.news.core.ui.interactions.domain.manager.ModalManager
import com.semenov.news.features.auth.domain.session.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Access to real persistence for end-to-end session tests; excluded from release builds. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthTestDependencies {
    fun sessionManager(): SessionManager
    fun storage(): Storage
    fun settingsStore(): AppSettingsStore
    fun database(): NewsDatabase
    fun modalManager(): ModalManager
}
