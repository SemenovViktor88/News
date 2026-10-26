package com.semenov.news.core.storage.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.semenov.news.core.storage.domain.Storage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@Singleton
class StorageImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : Storage {
    private val json = Json { ignoreUnknownKeys = true }

    override fun <T> getBlocking(
        key: String,
        defaultValue: T,
        serializer: KSerializer<T>,
    ): T = runBlocking { get(key, defaultValue, serializer) }

    override fun <T> putBlocking(
        key: String,
        value: T,
        serializer: KSerializer<T>,
    ) = runBlocking { put(key, value, serializer) }

    override suspend fun <T> get(
        key: String,
        defaultValue: T,
        serializer: KSerializer<T>,
    ): T = withContext(Dispatchers.IO) {
        val stored = dataStore.data.first().asMap()[storageKey(key)] as? String
            ?: return@withContext defaultValue
        try {
            json.decodeFromString(serializer, stored)
        } catch (_: IllegalArgumentException) {
            // Malformed JSON or an incompatible serializer uses the caller's default.
            defaultValue
        }
    }

    override suspend fun <T> put(
        key: String,
        value: T,
        serializer: KSerializer<T>,
    ) {
        withContext(Dispatchers.IO) {
            val encoded = json.encodeToString(serializer, value)
            dataStore.edit { it[storageKey(key)] = encoded }
        }
    }

    override suspend fun delete(key: String) {
        dataStore.edit { it.remove(storageKey(key)) }
    }

    override suspend fun deleteAll() {
        dataStore.edit { it.clear() }
    }
}

/** Keep existing preference names so saved settings survive updates. */
internal fun storageKey(key: String): Preferences.Key<String> = stringPreferencesKey("plain:$key")
