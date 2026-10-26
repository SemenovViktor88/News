package com.semenov.news.features.auth.domain.repository

import com.semenov.news.features.auth.domain.model.AuthChallenge
import com.semenov.news.features.auth.domain.model.AuthUser

interface AuthRepository {
    suspend fun signIn(userName: String, password: String): AuthChallenge

    suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthUser
}
