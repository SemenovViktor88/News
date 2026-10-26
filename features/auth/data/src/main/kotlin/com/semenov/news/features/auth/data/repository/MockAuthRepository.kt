package com.semenov.news.features.auth.data.repository

import com.semenov.news.features.auth.domain.model.AuthChallenge
import com.semenov.news.features.auth.domain.model.AuthException
import com.semenov.news.features.auth.domain.model.AuthUser
import com.semenov.news.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

/** Local demo authentication. No email is sent and no credentials are persisted. */
class MockAuthRepository @Inject constructor() : AuthRepository {
    override suspend fun signIn(userName: String, password: String): AuthChallenge {
        val normalizedName = userName.trim()
        if (!normalizedName.isValidUserName()) throw AuthException.InvalidUserName
        if (password != DEMO_PASSWORD) throw AuthException.InvalidCredentials
        return AuthChallenge(normalizedName)
    }

    override suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthUser {
        if (!challenge.userName.isValidUserName()) throw AuthException.InvalidUserName
        if (code != DEMO_CODE) throw AuthException.InvalidCode
        return AuthUser(challenge.userName)
    }

    private companion object {
        const val DEMO_PASSWORD = "news"
        const val DEMO_CODE = "1111"
    }
}

internal fun String.isValidUserName(): Boolean = isNotBlank() && codePointCount(0, length) <= 32
