package com.semenov.news.features.auth.data.repository

import com.semenov.news.features.auth.domain.model.AuthChallenge
import com.semenov.news.features.auth.domain.model.AuthException
import com.semenov.news.features.auth.domain.model.AuthUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class MockAuthRepositoryTest {
    private val repository = MockAuthRepository()

    @Test
    fun `correct demo credentials preserve the trimmed user name after code verification`() = runTest {
        val challenge = repository.signIn("  Віктор  ", "news")

        assertEquals(AuthChallenge("Віктор"), challenge)
        assertEquals(AuthUser("Віктор"), repository.verifyCode(challenge, "1111"))
    }

    @Test
    fun `name length is limited to 32 Unicode code points`() = runTest {
        assertEquals("😀".repeat(32), repository.signIn("😀".repeat(32), "news").userName)
        assertFailure(AuthException.InvalidUserName) {
            repository.signIn("😀".repeat(33), "news")
        }
        assertFailure(AuthException.InvalidUserName) { repository.signIn("  ", "news") }
    }

    @Test
    fun `password must match news exactly`() = runTest {
        for (password in listOf("", "News", "news ", "wrong")) {
            assertFailure(AuthException.InvalidCredentials) { repository.signIn("User", password) }
        }
    }

    @Test
    fun `verification accepts only the exact four digit demo code`() = runTest {
        val challenge = repository.signIn("User", "news")
        for (code in listOf("", "111", "11111", "0000", "1111 ")) {
            assertFailure(AuthException.InvalidCode) { repository.verifyCode(challenge, code) }
        }
    }

    private suspend fun assertFailure(expected: AuthException, action: suspend () -> Unit) {
        try {
            action()
            fail("Expected authentication failure")
        } catch (actual: AuthException) {
            assertEquals(expected, actual)
        }
    }
}
