package com.semenov.news.core.storage.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.storage.data.StorageImpl
import com.semenov.news.core.storage.data.settings.StorageAppSettingsStore
import com.semenov.news.core.storage.data.settings.appSettingsMigration
import com.semenov.news.core.storage.data.settings.hapticsSettingsMigration
import com.semenov.news.core.storage.domain.Storage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    @Binds
    abstract fun bindStorage(implementation: StorageImpl): Storage

    @Binds
    abstract fun bindAppSettingsStore(implementation: StorageAppSettingsStore): AppSettingsStore

    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                migrations = listOf(hapticsSettingsMigration(), appSettingsMigration(context)),
                produceFile = { context.preferencesDataStoreFile("news_storage") },
            )
    }
}
