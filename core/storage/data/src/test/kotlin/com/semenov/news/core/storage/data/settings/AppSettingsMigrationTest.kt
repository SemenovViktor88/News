package com.semenov.news.core.storage.data.settings

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import com.semenov.news.core.storage.data.storageKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsMigrationTest {
    @Test
    fun `old toggle is renamed without changing its value or unrelated settings`() = runTest {
        val legacyKey = storageKey("sounds_enabled")
        val hapticsKey = storageKey("haptics_enabled")
        val themeKey = storageKey("dark_theme")
        val migration = hapticsSettingsMigration()
        val preferences = preferencesOf(legacyKey to "false", themeKey to "true")

        assertTrue(migration.shouldMigrate(preferences))
        val migrated = migration.migrate(preferences)

        assertEquals("false", migrated[hapticsKey])
        assertEquals("true", migrated[themeKey])
        assertFalse(legacyKey in migrated)
        assertFalse(migration.shouldMigrate(migrated))
        assertEquals(migrated, migration.migrate(migrated))
    }

    @Test
    fun `existing haptic preference wins over an obsolete sound preference`() = runTest {
        val legacyKey = storageKey("sounds_enabled")
        val hapticsKey = storageKey("haptics_enabled")
        val preferences = preferencesOf(legacyKey to "false", hapticsKey to "true")

        val migrated = hapticsSettingsMigration().migrate(preferences)

        assertEquals("true", migrated[hapticsKey])
        assertFalse(legacyKey in migrated)
    }

    @Test
    fun `fresh storage needs no migration and keeps defaults unset`() = runTest {
        val migration = hapticsSettingsMigration()
        val preferences = emptyPreferences()

        assertFalse(migration.shouldMigrate(preferences))
        assertEquals(preferences, migration.migrate(preferences))
    }
}
