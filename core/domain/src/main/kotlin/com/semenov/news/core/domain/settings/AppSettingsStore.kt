package com.semenov.news.core.domain.settings

import kotlinx.coroutines.flow.StateFlow

data class AppSettings(
    val darkTheme: Boolean? = null,
    val hapticsEnabled: Boolean = true,
)

interface AppSettingsStore {
    val settings: StateFlow<AppSettings>

    fun read(): AppSettings

    suspend fun setDarkTheme(enabled: Boolean)

    suspend fun setHapticsEnabled(enabled: Boolean)
}
