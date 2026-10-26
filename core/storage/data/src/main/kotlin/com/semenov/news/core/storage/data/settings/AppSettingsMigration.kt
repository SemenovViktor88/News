package com.semenov.news.core.storage.data.settings

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import com.semenov.news.core.storage.data.storageKey

/** DataStore cleans the old keys only after the converted preferences are safely persisted. */
internal fun appSettingsMigration(
    context: Context,
    preferencesName: String = "app_settings",
): DataMigration<Preferences> = SharedPreferencesMigration(
    context = context,
    sharedPreferencesName = preferencesName,
    keysToMigrate = setOf("dark_theme", "sounds_enabled"),
    migrate = { legacy, current ->
        current.toMutablePreferences().apply {
            for ((key, value) in legacy.getAll()) {
                val destination = storageKey(if (key == "sounds_enabled") "haptics_enabled" else key)
                if (value is Boolean && destination !in this) {
                    this[destination] = value.toString()
                }
            }
        }
    },
)

/** Preserve the old toggle value while moving it to its haptic-specific preference key. */
internal fun hapticsSettingsMigration(): DataMigration<Preferences> = object : DataMigration<Preferences> {
    private val legacyKey = storageKey("sounds_enabled")
    private val hapticsKey = storageKey("haptics_enabled")

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = legacyKey in currentData

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            if (hapticsKey !in this) {
                currentData[legacyKey]?.let { this[hapticsKey] = it }
            }
            remove(legacyKey)
        }

    override suspend fun cleanUp() = Unit
}
