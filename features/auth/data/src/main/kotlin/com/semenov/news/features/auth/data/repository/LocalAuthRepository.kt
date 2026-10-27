package com.semenov.news.features.auth.data.repository

import com.semenov.news.features.auth.data.verification.AuthChallengeClock
import com.semenov.news.features.auth.data.verification.AuthCodeGenerator
import com.semenov.news.features.auth.data.verification.AuthCodeNotifier
import com.semenov.news.features.auth.domain.model.AuthChallenge
import com.semenov.news.features.auth.domain.model.AuthException
import com.semenov.news.features.auth.domain.model.AuthUser
import com.semenov.news.features.auth.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Local demo authentication. Verification codes live only in memory and in a notification. */
@Singleton
class LocalAuthRepository @Inject internal constructor(
    private val notifier: AuthCodeNotifier,
    private val codeGenerator: AuthCodeGenerator,
    private val clock: AuthChallengeClock,
) : AuthRepository {
    private val mutex = Mutex()
    private var pendingChallenge: PendingChallenge? = null
    private var previousCode: String? = null

    init {
        // A notification left by a previous process cannot be verified by this in-memory session.
        notifier.clear()
    }

    override suspend fun signIn(userName: String, password: String): AuthChallenge = mutex.withLock {
        val normalizedName = userName.trim()
        if (!normalizedName.isValidUserName()) throw AuthException.InvalidUserName
        if (password != DEMO_PASSWORD) throw AuthException.InvalidCredentials
        issueCode(normalizedName)
    }

    override suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthUser = mutex.withLock {
        val pending = currentChallenge(challenge)
        if (clock.nowMillis() - pending.issuedAtMillis >= CODE_VALIDITY_MILLIS) {
            notifier.clear()
            throw AuthException.InvalidCode
        }
        if (code != pending.code) throw AuthException.InvalidCode

        pendingChallenge = null
        notifier.clear()
        AuthUser(challenge.userName)
    }

    override suspend fun resendCode(challenge: AuthChallenge): AuthChallenge = mutex.withLock {
        currentChallenge(challenge)
        issueCode(challenge.userName)
    }

    override suspend fun cancelChallenge(challenge: AuthChallenge) = mutex.withLock {
        if (pendingChallenge?.challenge == challenge) {
            pendingChallenge = null
            notifier.clear()
        }
    }

    private fun currentChallenge(challenge: AuthChallenge): PendingChallenge =
        pendingChallenge?.takeIf { it.challenge == challenge } ?: throw AuthException.InvalidCode

    private fun issueCode(userName: String): AuthChallenge {
        // A new attempt invalidates the previous one even if notification delivery is unavailable.
        pendingChallenge = null
        notifier.clear()
        val code = codeGenerator.generate(excluding = previousCode)
        if (!notifier.show(code)) throw AuthException.NotificationsUnavailable

        val challenge = AuthChallenge(userName)
        previousCode = code
        pendingChallenge = PendingChallenge(challenge, code, clock.nowMillis())
        return challenge
    }

    private class PendingChallenge(
        val challenge: AuthChallenge,
        val code: String,
        val issuedAtMillis: Long,
    )

    private companion object {
        const val DEMO_PASSWORD = "news"
    }
}

internal const val CODE_VALIDITY_MILLIS = 5 * 60 * 1_000L

internal fun String.isValidUserName(): Boolean = isNotBlank() && codePointCount(0, length) <= 32
