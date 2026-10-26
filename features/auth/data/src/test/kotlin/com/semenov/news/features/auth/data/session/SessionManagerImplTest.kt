package com.semenov.news.features.auth.data.session

import com.semenov.news.core.db.NewsCacheCoordinator
import com.semenov.news.core.domain.settings.AppSettings
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.storage.domain.Storage
import com.semenov.news.core.storage.domain.getBlocking
import com.semenov.news.core.storage.domain.put
import com.semenov.news.features.auth.domain.model.AuthUser
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerImplTest {
    @Test
    fun `fresh installation has no user and cache access stays disabled`() {
        val coordinator = NewsCacheCoordinator()
        val manager = manager(coordinator = coordinator)

        assertNull(manager.user.value)
        assertNull(coordinator.currentGeneration())
    }

    @Test
    fun `saved user is persisted and restored before navigation starts`() = runTest {
        val storage = FakeStorage()
        val original = manager(storage)
        val user = AuthUser("Віктор")

        original.saveUser(user)
        val coordinator = NewsCacheCoordinator()
        val restored = manager(storage, coordinator = coordinator)

        assertEquals(user, original.user.value)
        assertEquals(user, restored.user.value)
        assertNotNull(coordinator.currentGeneration())
        assertEquals(setOf("auth_user"), storage.values.keys)
    }

    @Test
    fun `failed persistence cannot publish an authenticated session`() = runTest {
        val storage = FakeStorage().apply { failWrites = true }
        val coordinator = NewsCacheCoordinator()
        val manager = manager(storage, coordinator = coordinator)

        try {
            manager.saveUser(AuthUser("User"))
            fail("Expected persistence failure")
        } catch (_: IOException) {
            assertNull(manager.user.value)
            assertNull(coordinator.currentGeneration())
        }
    }

    @Test
    fun `logout clears all preferences database and cached settings before publishing no user`() = runTest {
        val storage = FakeStorage()
        val settings = FakeSettingsStore(storage)
        val coordinator = NewsCacheCoordinator()
        var databaseCleared = false
        val manager = manager(storage, settings, coordinator) { databaseCleared = true }
        manager.saveUser(AuthUser("User"))
        settings.setDarkTheme(true)
        settings.setHapticsEnabled(false)
        storage.put("unrelated_value", "also removed")

        manager.logout()

        assertTrue(databaseCleared)
        assertTrue(storage.values.isEmpty())
        assertEquals(AppSettings(), settings.read())
        assertNull(manager.user.value)
        assertNull(manager(storage).user.value)
        assertNull(coordinator.currentGeneration())
    }

    @Test
    fun `failed database cleanup keeps the session and allows logout to be retried`() = runTest {
        val storage = FakeStorage()
        val coordinator = NewsCacheCoordinator()
        var shouldFail = true
        val manager = manager(storage, coordinator = coordinator) {
            if (shouldFail) throw IOException("Database unavailable")
        }
        val user = AuthUser("User")
        manager.saveUser(user)
        val oldGeneration = checkNotNull(coordinator.currentGeneration())

        try {
            manager.logout()
            fail("Expected cleanup failure")
        } catch (_: IOException) {
            assertEquals(user, manager.user.value)
            assertEquals(user, storage.getBlocking<AuthUser?>("auth_user", null))
            assertNotNull(coordinator.currentGeneration())
            assertNull(coordinator.withSession(oldGeneration) { error("Earlier requests must stay invalid") })
            assertEquals("usable", coordinator.withSession(checkNotNull(coordinator.currentGeneration())) { "usable" })
        }
        shouldFail = false
        manager.logout()
        assertNull(manager.user.value)
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun `failed preference cleanup restores cache access for the retained user only`() = runTest {
        val storage = FakeStorage()
        val coordinator = NewsCacheCoordinator()
        val manager = manager(storage, coordinator = coordinator)
        val user = AuthUser("User")
        manager.saveUser(user)
        val oldGeneration = checkNotNull(coordinator.currentGeneration())
        storage.failClear = true

        try {
            manager.logout()
            fail("Expected cleanup failure")
        } catch (_: IOException) {
            assertEquals(user, manager.user.value)
            assertEquals(user, storage.getBlocking<AuthUser?>("auth_user", null))
            assertNotNull(coordinator.currentGeneration())
            assertNull(coordinator.withSession(oldGeneration) { error("Earlier requests must stay invalid") })
        }

        storage.failClear = false
        manager.logout()
        assertNull(manager.user.value)
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun `cancelling logout screen does not interrupt cleanup or leave a saved user`() = runTest {
        val storage = FakeStorage()
        val releaseCleanup = CompletableDeferred<Unit>()
        var cleared = false
        val manager = manager(storage) {
            releaseCleanup.await()
            cleared = true
        }
        manager.saveUser(AuthUser("User"))
        val logout = launch { manager.logout() }
        runCurrent()

        logout.cancel()
        runCurrent()
        assertFalse(cleared)
        releaseCleanup.complete(Unit)
        logout.join()

        assertTrue(cleared)
        assertNull(manager.user.value)
        assertTrue(storage.values.isEmpty())
    }

    private fun manager(
        storage: FakeStorage = FakeStorage(),
        settings: AppSettingsStore = FakeSettingsStore(storage),
        coordinator: NewsCacheCoordinator = NewsCacheCoordinator(),
        clearDatabase: suspend () -> Unit = {},
    ) = SessionManagerImpl(storage, settings, SessionDataCleaner(clearDatabase), coordinator)

    private class FakeSettingsStore(private val storage: Storage) : AppSettingsStore {
        override val settings = MutableStateFlow(AppSettings())
        override fun read(): AppSettings = settings.value
        override suspend fun setDarkTheme(enabled: Boolean) {
            storage.put("dark_theme", enabled)
            settings.value = read().copy(darkTheme = enabled)
        }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            storage.put("haptics_enabled", enabled)
            settings.value = read().copy(hapticsEnabled = enabled)
        }

        override suspend fun clear() {
            storage.deleteAll()
            settings.value = AppSettings()
        }
    }

    private class FakeStorage : Storage {
        val values = mutableMapOf<String, String>()
        var failWrites = false
        var failClear = false

        override fun <T> getBlocking(key: String, defaultValue: T, serializer: KSerializer<T>): T =
            values[key]?.let { Json.decodeFromString(serializer, it) } ?: defaultValue

        override fun <T> putBlocking(key: String, value: T, serializer: KSerializer<T>) {
            if (failWrites) throw IOException("Storage unavailable")
            values[key] = Json.encodeToString(serializer, value)
        }

        override suspend fun <T> get(key: String, defaultValue: T, serializer: KSerializer<T>): T =
            getBlocking(key, defaultValue, serializer)

        override suspend fun <T> put(key: String, value: T, serializer: KSerializer<T>) =
            putBlocking(key, value, serializer)

        override suspend fun delete(key: String) {
            values.remove(key)
        }

        override suspend fun deleteAll() {
            if (failClear) throw IOException("Storage unavailable")
            values.clear()
        }
    }
}
