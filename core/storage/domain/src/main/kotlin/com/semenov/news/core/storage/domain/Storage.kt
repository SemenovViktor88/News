package com.semenov.news.core.storage.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Persistent values encoded with the supplied serializers. Reads use the default for absent or
 * undecodable values; storage IO failures propagate. Blocking methods wait for completion;
 * use suspend methods from coroutines.
 */
interface Storage {
    fun <T> getBlocking(
        key: String,
        defaultValue: T,
        serializer: KSerializer<T>,
    ): T

    fun <T> putBlocking(
        key: String,
        value: T,
        serializer: KSerializer<T>,
    )

    suspend fun <T> get(
        key: String,
        defaultValue: T,
        serializer: KSerializer<T>,
    ): T

    suspend fun <T> put(
        key: String,
        value: T,
        serializer: KSerializer<T>,
    )

    suspend fun delete(key: String)

    suspend fun deleteAll()
}

inline fun <reified T> Storage.getBlocking(
    key: String,
    defaultValue: T,
): T = getBlocking(key, defaultValue, serializer<T>())

inline fun <reified T> Storage.putBlocking(
    key: String,
    value: T,
) = putBlocking(key, value, serializer<T>())

suspend inline fun <reified T> Storage.get(
    key: String,
    defaultValue: T,
): T = get(key, defaultValue, serializer<T>())

suspend inline fun <reified T> Storage.put(
    key: String,
    value: T,
) = put(key, value, serializer<T>())
