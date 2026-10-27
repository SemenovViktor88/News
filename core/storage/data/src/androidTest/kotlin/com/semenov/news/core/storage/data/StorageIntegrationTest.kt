package com.semenov.news.core.storage.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.semenov.news.core.storage.data.settings.StorageAppSettingsStore
import com.semenov.news.core.storage.data.settings.appSettingsMigration
import com.semenov.news.core.storage.data.settings.hapticsSettingsMigration
import com.semenov.news.core.storage.domain.get
import com.semenov.news.core.storage.domain.put
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val id = UUID.randomUUID().toString()
    private val legacyName = "storage_migration_test_$id"
    private val file = File(context.cacheDir, "storage_test_$id.preferences_pb")
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var storage: StorageImpl

    @Before
    fun setUp() {
        openStorage()
    }

    @After
    fun tearDown() = runBlocking {
        closeStorage()
        file.delete()
        context.deleteSharedPreferences(legacyName)
        Unit
    }

    @Test
    fun legacyPreferencesMigrateAndStayDeletedAfterClearingStorage() = runBlocking {
        // DataStore initializes lazily, so seed the legacy file before the first read.
        val legacy = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        assertTrue(
            legacy.edit()
                .putBoolean("dark_theme", false)
                .putBoolean("sounds_enabled", false)
                .putString("unrelated", "keep")
                .commit(),
        )

        val settings = StorageAppSettingsStore(storage).read()
        assertEquals(false, settings.darkTheme)
        assertFalse(settings.hapticsEnabled)
        assertFalse(storage.get("haptics_enabled", true))
        assertFalse(dataStore.data.first().contains(storageKey("sounds_enabled")))
        assertFalse(legacy.contains("dark_theme"))
        assertFalse(legacy.contains("sounds_enabled"))
        assertEquals("keep", legacy.getString("unrelated", null))

        storage.deleteAll()
        closeStorage()
        openStorage()

        val clearedSettings = StorageAppSettingsStore(storage).read()
        assertNull(clearedSettings.darkTheme)
        assertTrue(clearedSettings.hapticsEnabled)
    }

    @Test
    fun migrationKeepsAlreadyPersistedValues() = runBlocking {
        storage.put("dark_theme", true)
        storage.put("haptics_enabled", true)
        storage.put("sounds_enabled", false)
        closeStorage()
        assertTrue(
            context.getSharedPreferences(legacyName, Context.MODE_PRIVATE).edit()
                .putBoolean("dark_theme", false)
                .putBoolean("sounds_enabled", false)
                .commit(),
        )
        openStorage()

        val settings = StorageAppSettingsStore(storage).read()
        assertEquals(true, settings.darkTheme)
        assertTrue(settings.hapticsEnabled)
        assertFalse(dataStore.data.first().contains(storageKey("sounds_enabled")))
        assertFalse(context.getSharedPreferences(legacyName, Context.MODE_PRIVATE).contains("dark_theme"))
    }

    @Test
    fun persistedSoundPreferenceMigratesToHapticsAndSurvivesRecreation() = runBlocking {
        storage.put("sounds_enabled", false)
        closeStorage()
        // A stale legacy preference must not replace the value already in DataStore.
        assertTrue(
            context.getSharedPreferences(legacyName, Context.MODE_PRIVATE).edit()
                .putBoolean("sounds_enabled", true)
                .commit(),
        )
        openStorage()

        assertFalse(StorageAppSettingsStore(storage).read().hapticsEnabled)
        assertFalse(storage.get("haptics_enabled", true))
        assertFalse(dataStore.data.first().contains(storageKey("sounds_enabled")))
        assertFalse(context.getSharedPreferences(legacyName, Context.MODE_PRIVATE).contains("sounds_enabled"))

        closeStorage()
        openStorage()

        assertFalse(StorageAppSettingsStore(storage).read().hapticsEnabled)
        assertFalse(dataStore.data.first().contains(storageKey("sounds_enabled")))
    }

    @Test
    fun serializedValuesSurviveRecreationAndDeleteRemovesThem() = runBlocking {
        val key = "collection_$id"
        val value = listOf("Новини", "android", "kotlin")
        storage.put(key, value)
        assertTrue(dataStore.data.first().contains(storageKey(key)))

        closeStorage()
        openStorage()
        assertEquals(value, storage.get(key, emptyList<String>()))
        storage.delete(key)
        assertEquals(emptyList<String>(), storage.get(key, emptyList<String>()))
        assertFalse(dataStore.data.first().contains(storageKey(key)))
    }

    @Test
    fun preferencesWrittenByDataStore121SurviveUpgradeAndClearStaysPersisted() = runBlocking {
        closeStorage()
        // Frozen protobuf produced by the 1.2.1 PreferencesProto writer, before this upgrade.
        // Reading a fixture catches format regressions that a round trip with one version cannot.
        InstrumentationRegistry.getInstrumentation().context.assets
            .open("datastore-1.2.1.preferences_pb")
            .use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        openStorage()

        assertEquals(mapOf("name" to "Віктор"), storage.get("auth_user", emptyMap<String, String>()))
        assertEquals(listOf("Новини", "android"), storage.get("bookmarks", emptyList<String>()))
        assertEquals(false, StorageAppSettingsStore(storage).read().darkTheme)
        assertFalse(StorageAppSettingsStore(storage).read().hapticsEnabled)

        storage.put("dark_theme", true)
        closeStorage()
        openStorage()

        assertEquals(mapOf("name" to "Віктор"), storage.get("auth_user", emptyMap<String, String>()))
        assertEquals(true, StorageAppSettingsStore(storage).read().darkTheme)
        assertFalse(StorageAppSettingsStore(storage).read().hapticsEnabled)

        storage.deleteAll()
        closeStorage()
        openStorage()

        assertTrue(dataStore.data.first().asMap().isEmpty())
        assertNull(storage.get<Map<String, String>?>("auth_user", null))
        assertNull(StorageAppSettingsStore(storage).read().darkTheme)
        assertTrue(StorageAppSettingsStore(storage).read().hapticsEnabled)
    }

    private fun openStorage() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            migrations = listOf(hapticsSettingsMigration(), appSettingsMigration(context, legacyName)),
            produceFile = { file },
        )
        storage = StorageImpl(dataStore)
    }

    private suspend fun closeStorage() {
        scope.coroutineContext[Job]!!.cancelAndJoin()
    }
}
