package com.semenov.news.core.storage.data.settings

import com.semenov.news.core.domain.settings.AppSettings
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.storage.domain.Storage
import com.semenov.news.core.storage.domain.getBlocking
import com.semenov.news.core.storage.domain.put
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class StorageAppSettingsStore @Inject constructor(
    private val storage: Storage,
) : AppSettingsStore {
    private val writes = Mutex()
    private val currentSettings = MutableStateFlow(
        AppSettings(
            darkTheme = storage.getBlocking<Boolean?>(DARK_THEME, null),
            hapticsEnabled = storage.getBlocking(HAPTICS_ENABLED, true),
        ),
    )

    override val settings = currentSettings.asStateFlow()

    override fun read(): AppSettings = settings.value

    override suspend fun setDarkTheme(enabled: Boolean) {
        writes.withLock {
            storage.put(DARK_THEME, enabled)
            currentSettings.update { it.copy(darkTheme = enabled) }
        }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        writes.withLock {
            storage.put(HAPTICS_ENABLED, enabled)
            currentSettings.update { it.copy(hapticsEnabled = enabled) }
        }
    }

    override suspend fun clear() {
        writes.withLock {
            storage.deleteAll()
            currentSettings.value = AppSettings()
        }
    }

    private companion object {
        const val DARK_THEME = "dark_theme"
        const val HAPTICS_ENABLED = "haptics_enabled"
    }
}
