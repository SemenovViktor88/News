package com.semenov.news.core.db

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keeps responses started by an earlier session from restoring the cache after logout. */
@Singleton
class NewsCacheCoordinator @Inject constructor() {
    private val cacheAccess = Mutex()
    private val stateLock = Any()
    private var generation = 0L
    private var active = false

    fun currentGeneration(): Long? = synchronized(stateLock) {
        generation.takeIf { active }
    }

    /** Called before publishing a restored or newly authenticated user. */
    fun startSession() {
        synchronized(stateLock) {
            generation++
            active = true
        }
    }

    suspend fun <T> withSession(expectedGeneration: Long, action: suspend () -> T): T? =
        cacheAccess.withLock {
            val isCurrent = synchronized(stateLock) {
                active && generation == expectedGeneration
            }
            if (isCurrent) action() else null
        }

    suspend fun clearForLogout(clear: suspend () -> Unit) {
        cacheAccess.withLock {
            synchronized(stateLock) {
                generation++
                active = false
            }
            clear()
        }
    }
}
