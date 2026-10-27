package com.semenov.news.features.auth.data.session

import com.semenov.news.core.db.NewsCacheCoordinator
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.storage.domain.Storage
import com.semenov.news.core.storage.domain.getBlocking
import com.semenov.news.core.storage.domain.put
import com.semenov.news.features.auth.domain.model.AuthUser
import com.semenov.news.features.auth.domain.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class SessionManagerImpl @Inject constructor(
    private val storage: Storage,
    private val settingsStore: AppSettingsStore,
    private val dataCleaner: SessionDataCleaner,
    private val cacheCoordinator: NewsCacheCoordinator,
) : SessionManager {
    private val writes = Mutex()
    private val currentUser = MutableStateFlow(storage.getBlocking<AuthUser?>(USER, null))

    override val user = currentUser.asStateFlow()

    init {
        if (currentUser.value != null) cacheCoordinator.startSession()
    }

    override suspend fun saveUser(user: AuthUser) {
        writes.withLock {
            storage.put(USER, user)
            cacheCoordinator.startSession()
            currentUser.value = user
        }
    }

    override suspend fun logout() {
        writes.withLock {
            // Finish both cleanups even when leaving the regular flow cancels its screen scope.
            withContext(NonCancellable) {
                try {
                    cacheCoordinator.clearForLogout {
                        dataCleaner.clearDatabase()
                        settingsStore.clear()
                    }
                    currentUser.value = null
                } catch (error: Exception) {
                    if (error !is CancellationException && currentUser.value != null) {
                        // Keep the retained session usable while rejecting its earlier requests.
                        cacheCoordinator.startSession()
                    }
                    throw error
                }
            }
        }
    }

    private companion object {
        const val USER = "auth_user"
    }
}
