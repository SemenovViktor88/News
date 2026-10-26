package com.semenov.news.core.storage.data.settings

import com.semenov.news.core.domain.settings.AppSettings
import com.semenov.news.core.storage.domain.Storage
import com.semenov.news.core.storage.domain.getBlocking
import com.semenov.news.core.storage.domain.putBlocking
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageAppSettingsStoreTest {
    @Test
    fun `missing preferences follow system theme and enable haptics without writing defaults`() {
        val storage = FakeStorage()

        val store = StorageAppSettingsStore(storage)

        assertEquals(AppSettings(), store.read())
        assertEquals(store.read(), store.settings.value)
        assertTrue(storage.writes.isEmpty())
    }

    @Test
    fun `saved false preferences are available immediately after recreation`() {
        val storage = FakeStorage().apply {
            putBlocking("dark_theme", false)
            putBlocking("haptics_enabled", false)
        }

        val store = StorageAppSettingsStore(storage)

        assertEquals(AppSettings(darkTheme = false, hapticsEnabled = false), store.read())
        assertEquals(store.read(), store.settings.value)
    }

    @Test
    fun `haptic changes preserve an unset theme and survive recreation`() = runTest {
        val storage = FakeStorage()
        val store = StorageAppSettingsStore(storage)

        store.setHapticsEnabled(false)

        assertEquals(AppSettings(hapticsEnabled = false), store.read())
        assertEquals(store.read(), StorageAppSettingsStore(storage).read())
        assertFalse(storage.getBlocking("haptics_enabled", true))
    }

    @Test
    fun `concurrent changes publish only committed preferences and preserve both values`() = runTest {
        val storage = FakeStorage()
        val writeReady = CompletableDeferred<Unit>()
        storage.beforePut = { key -> if (key == "dark_theme") writeReady.await() }
        val store = StorageAppSettingsStore(storage)

        val themeWrite = launch { store.setDarkTheme(true) }
        runCurrent()
        val hapticsWrite = launch { store.setHapticsEnabled(false) }
        runCurrent()

        assertEquals(AppSettings(), store.settings.value)
        assertEquals(listOf("dark_theme"), storage.writes)
        assertEquals(AppSettings(), StorageAppSettingsStore(storage).read())

        writeReady.complete(Unit)
        joinAll(themeWrite, hapticsWrite)

        assertEquals(AppSettings(darkTheme = true, hapticsEnabled = false), store.read())
        assertEquals(store.read(), StorageAppSettingsStore(storage).read())
        assertEquals(listOf("dark_theme", "haptics_enabled"), storage.writes)
    }

    @Test
    fun `failed persistence leaves the published preferences unchanged`() = runTest {
        val storage = FakeStorage().apply {
            beforePut = { throw IOException("Storage unavailable") }
        }
        val store = StorageAppSettingsStore(storage)

        try {
            store.setDarkTheme(true)
            fail("Expected the write failure to propagate")
        } catch (_: IOException) {
            assertEquals(AppSettings(), store.read())
            assertEquals(AppSettings(), StorageAppSettingsStore(storage).read())
        }
    }

    @Test
    fun `cancelled write releases the lock without publishing an uncommitted value`() = runTest {
        val storage = FakeStorage()
        val writeReady = CompletableDeferred<Unit>()
        storage.beforePut = { writeReady.await() }
        val store = StorageAppSettingsStore(storage)

        val themeWrite = launch { store.setDarkTheme(true) }
        runCurrent()
        themeWrite.cancelAndJoin()
        storage.beforePut = {}
        store.setHapticsEnabled(false)

        assertEquals(AppSettings(hapticsEnabled = false), store.read())
        assertEquals(store.read(), StorageAppSettingsStore(storage).read())
    }

    private class FakeStorage : Storage {
        private val json = Json
        private val values = mutableMapOf<String, String>()
        val writes = mutableListOf<String>()
        var beforePut: suspend (String) -> Unit = {}

        override fun <T> getBlocking(
            key: String,
            defaultValue: T,
            serializer: KSerializer<T>,
        ): T {
            val serialized = values[key] ?: return defaultValue
            return json.decodeFromString(serializer, serialized)
        }

        override fun <T> putBlocking(
            key: String,
            value: T,
            serializer: KSerializer<T>,
        ) {
            values[key] = json.encodeToString(serializer, value)
        }

        override suspend fun <T> get(
            key: String,
            defaultValue: T,
            serializer: KSerializer<T>,
        ): T = getBlocking(key, defaultValue, serializer)

        override suspend fun <T> put(
            key: String,
            value: T,
            serializer: KSerializer<T>,
        ) {
            writes += key
            beforePut(key)
            putBlocking(key, value, serializer)
        }

        override suspend fun delete(key: String) {
            values.remove(key)
        }

        override suspend fun deleteAll() {
            values.clear()
        }
    }
}
