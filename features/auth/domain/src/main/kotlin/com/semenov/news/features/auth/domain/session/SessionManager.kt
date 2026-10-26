package com.semenov.news.features.auth.domain.session

import com.semenov.news.features.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

interface SessionManager {
    val user: StateFlow<AuthUser?>

    suspend fun saveUser(user: AuthUser)

    /** Removes the saved user, all preferences, and all local database tables. */
    suspend fun logout()
}
