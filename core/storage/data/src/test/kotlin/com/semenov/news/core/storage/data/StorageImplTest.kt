package com.semenov.news.core.storage.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.semenov.news.core.storage.domain.Storage
import com.semenov.news.core.storage.domain.get
import com.semenov.news.core.storage.domain.getBlocking
import com.semenov.news.core.storage.domain.put
import com.semenov.news.core.storage.domain.putBlocking
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StorageImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun closeDataStores() = runBlocking {
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test
    fun `missing values return caller defaults without creating records`() = runBlocking {
        val fixture = createStorage()

        assertEquals(42, fixture.storage.get("missing", 42))
        assertNull(fixture.storage.get<String?>("missing-null", null))
        assertTrue(fixture.dataStore.data.first().asMap().isEmpty())
    }

    @Test
    fun `serializers round trip primitives nullable collections and objects`() = runBlocking {
        val storage = createStorage().storage
        storage.put("boolean", false)
        storage.put("int", Int.MIN_VALUE)
        storage.put("long", Long.MAX_VALUE)
        storage.put("float", 1.25f)
        storage.put("double", 1.125)
        storage.put("string", "Новини 🔔")
        storage.put<String?>("nullable", null)
        storage.put("list", listOf("first", "second"))
        val profile = Profile(id = 7, tags = listOf("android", "kotlin"))
        storage.put("object", profile)

        assertFalse(storage.get("boolean", true))
        assertEquals(Int.MIN_VALUE, storage.get("int", 0))
        assertEquals(Long.MAX_VALUE, storage.get("long", 0L))
        assertEquals(1.25f, storage.get("float", 0f))
        assertEquals(1.125, storage.get("double", 0.0), 0.0)
        assertEquals("Новини 🔔", storage.get("string", ""))
        assertNull(storage.get<String?>("nullable", "fallback"))
        assertEquals(listOf("first", "second"), storage.get("list", emptyList<String>()))
        assertEquals(profile, storage.get("object", Profile(0, emptyList())))
    }

    @Test
    fun `blocking put completes before returning and persists across DataStore recreation`() = runBlocking {
        val original = createStorage()

        original.storage.putBlocking("ready", "persisted")

        assertEquals("persisted", original.storage.getBlocking("ready", "missing"))
        assertEquals("\"persisted\"", original.dataStore.data.first()[stringPreferencesKey("plain:ready")])
        original.scope.coroutineContext[Job]!!.cancelAndJoin()
        val reopened = createStorage(original.file)

        assertEquals("persisted", reopened.storage.getBlocking("ready", "missing"))
    }

    @Test
    fun `delete removes only its value and deleteAll clears all remaining values`() = runBlocking {
        val fixture = createStorage()
        fixture.storage.put("removed", true)
        fixture.storage.put("retained", 7)

        fixture.storage.delete("removed")

        assertFalse(fixture.dataStore.data.first().contains(stringPreferencesKey("plain:removed")))
        assertEquals(7, fixture.storage.get("retained", 0))

        fixture.storage.deleteAll()

        assertTrue(fixture.dataStore.data.first().asMap().isEmpty())
        assertEquals(0, fixture.storage.get("retained", 0))
    }

    @Test
    fun `malformed serialized values and incompatible types use caller defaults`() = runBlocking {
        val fixture = createStorage()
        fixture.dataStore.edit {
            it[stringPreferencesKey("plain:invalid-json")] = "not JSON"
            it[stringPreferencesKey("plain:wrong-type")] = "[1,2,3]"
        }

        assertEquals(11, fixture.storage.get("invalid-json", 11))
        assertEquals(false, fixture.storage.get("wrong-type", false))
    }

    @Test
    fun `storage IO failures propagate to callers instead of returning defaults`() = runBlocking {
        val storage = StorageImpl(FailingDataStore(IOException("Disk unavailable")))

        expectFailure<IOException> { storage.get("key", "fallback") }
        expectFailure<IOException> { storage.put("key", "value") }
    }

    @Test
    fun `cancellation propagates instead of returning a default`() = runBlocking {
        val storage = StorageImpl(FailingDataStore(CancellationException("Cancelled read")))

        expectFailure<CancellationException> { storage.get("key", "fallback") }
    }

    private fun createStorage(file: File = File(temporaryFolder.root, "storage-${scopes.size}.preferences_pb")): Fixture {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also(scopes::add)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        return Fixture(StorageImpl(dataStore), dataStore, scope, file)
    }

    private data class Fixture(
        val storage: Storage,
        val dataStore: DataStore<Preferences>,
        val scope: CoroutineScope,
        val file: File,
    )

    @Serializable
    private data class Profile(val id: Int, val tags: List<String>)

    private class FailingDataStore(private val failure: Exception) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw failure }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw failure
    }

    private suspend inline fun <reified T : Throwable> expectFailure(crossinline action: suspend () -> Unit) {
        try {
            action()
            fail("Expected ${T::class.simpleName}")
        } catch (failure: Throwable) {
            if (failure !is T) throw failure
        }
    }
}
