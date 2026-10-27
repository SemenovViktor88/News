package com.semenov.news.features.auth.data.repository

import com.semenov.news.features.auth.data.verification.AuthChallengeClock
import com.semenov.news.features.auth.data.verification.AuthCodeGenerator
import com.semenov.news.features.auth.data.verification.AuthCodeNotifier
import com.semenov.news.features.auth.domain.model.AuthChallenge
import com.semenov.news.features.auth.domain.model.AuthException
import com.semenov.news.features.auth.domain.model.AuthUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class LocalAuthRepositoryTest {
    private val notifier = FakeNotifier()
    private val generator = FakeGenerator()
    private val clock = FakeClock()
    private val repository = LocalAuthRepository(notifier, generator, clock)

    @Test
    fun `valid credentials deliver a new code and preserve trimmed user name`() = runTest {
        generator.nextCode = "0007"
        val challenge = repository.signIn("  Віктор  ", "news")

        assertEquals("Віктор", challenge.userName)
        assertEquals("0007", notifier.code)
        assertEquals(AuthUser("Віктор"), repository.verifyCode(challenge, "0007"))
        assertNull(notifier.code)
    }

    @Test
    fun `name length is limited to 32 Unicode code points`() = runTest {
        assertEquals("😀".repeat(32), repository.signIn("😀".repeat(32), "news").userName)
        assertFailure(AuthException.InvalidUserName) { repository.signIn("😀".repeat(33), "news") }
        assertFailure(AuthException.InvalidUserName) { repository.signIn("  ", "news") }
    }

    @Test
    fun `bad password never generates or delivers a code`() = runTest {
        for (password in listOf("", "News", "news ", "wrong")) {
            assertFailure(AuthException.InvalidCredentials) { repository.signIn("User", password) }
        }
        assertEquals(0, generator.generatedCount)
        assertEquals(0, notifier.shownCount)
    }

    @Test
    fun `new sign in invalidates previous challenge and excludes its code`() = runTest {
        generator.nextCode = "1234"
        val oldChallenge = repository.signIn("User", "news")
        generator.nextCode = "5678"
        val newChallenge = repository.signIn("User", "news")

        assertNotEquals(oldChallenge.id, newChallenge.id)
        assertEquals("1234", generator.excludedCode)
        assertEquals("5678", notifier.code)
        assertFailure(AuthException.InvalidCode) { repository.verifyCode(oldChallenge, "1234") }
        assertFailure(AuthException.InvalidCode) { repository.verifyCode(newChallenge, "1234") }
        assertEquals(AuthUser("User"), repository.verifyCode(newChallenge, "5678"))
    }

    @Test
    fun `resending replaces challenge and makes old code unusable`() = runTest {
        generator.nextCode = "1234"
        val oldChallenge = repository.signIn("User", "news")
        generator.nextCode = "5678"
        val newChallenge = repository.resendCode(oldChallenge)

        assertNotEquals(oldChallenge.id, newChallenge.id)
        assertEquals("1234", generator.excludedCode)
        assertFailure(AuthException.InvalidCode) { repository.verifyCode(oldChallenge, "1234") }
        assertFailure(AuthException.InvalidCode) { repository.verifyCode(newChallenge, "1234") }
        assertEquals(AuthUser("User"), repository.verifyCode(newChallenge, "5678"))
    }

    @Test
    fun `wrong and malformed codes do not consume valid challenge`() = runTest {
        generator.nextCode = "0007"
        val challenge = repository.signIn("User", "news")

        for (code in listOf("", "007", "00070", "1111", "0007 ")) {
            assertFailure(AuthException.InvalidCode) { repository.verifyCode(challenge, code) }
        }
        assertEquals("0007", notifier.code)
        assertEquals(AuthUser("User"), repository.verifyCode(challenge, "0007"))
    }

    @Test
    fun `a successfully verified challenge cannot be used or resent again`() = runTest {
        val challenge = repository.signIn("User", "news")
        repository.verifyCode(challenge, generator.nextCode)

        assertFailure(AuthException.InvalidCode) { repository.verifyCode(challenge, generator.nextCode) }
        assertFailure(AuthException.InvalidCode) { repository.resendCode(challenge) }
        assertNull(notifier.code)
    }

    @Test
    fun `code expires at five minutes but user can request a replacement`() = runTest {
        val challenge = repository.signIn("User", "news")
        clock.timeMillis += CODE_VALIDITY_MILLIS

        assertFailure(AuthException.InvalidCode) { repository.verifyCode(challenge, generator.nextCode) }
        assertNull(notifier.code)
        generator.nextCode = "5678"
        val replacement = repository.resendCode(challenge)
        assertEquals(AuthUser("User"), repository.verifyCode(replacement, "5678"))
    }

    @Test
    fun `code remains valid immediately before expiry`() = runTest {
        val challenge = repository.signIn("User", "news")
        clock.timeMillis += CODE_VALIDITY_MILLIS - 1

        assertEquals(AuthUser("User"), repository.verifyCode(challenge, generator.nextCode))
    }

    @Test
    fun `unknown or changed challenge cannot verify or resend`() = runTest {
        val challenge = repository.signIn("User", "news")
        for (invalid in listOf(AuthChallenge("User"), challenge.copy(userName = "Another user"))) {
            assertFailure(AuthException.InvalidCode) { repository.verifyCode(invalid, generator.nextCode) }
            assertFailure(AuthException.InvalidCode) { repository.resendCode(invalid) }
        }
        assertEquals(AuthUser("User"), repository.verifyCode(challenge, generator.nextCode))
    }

    @Test
    fun `cancelling current challenge clears notification and invalidates code`() = runTest {
        val challenge = repository.signIn("User", "news")
        repository.cancelChallenge(challenge)

        assertNull(notifier.code)
        assertFailure(AuthException.InvalidCode) { repository.verifyCode(challenge, generator.nextCode) }
    }

    @Test
    fun `cancelling old challenge leaves latest attempt active`() = runTest {
        val oldChallenge = repository.signIn("User", "news")
        generator.nextCode = "5678"
        val newChallenge = repository.resendCode(oldChallenge)
        repository.cancelChallenge(oldChallenge)

        assertEquals("5678", notifier.code)
        assertEquals(AuthUser("User"), repository.verifyCode(newChallenge, "5678"))
    }

    @Test
    fun `disabled notifications fail sign in without leaving a usable challenge`() = runTest {
        val oldChallenge = repository.signIn("User", "news")
        notifier.available = false

        assertFailure(AuthException.NotificationsUnavailable) { repository.signIn("User", "news") }
        assertNull(notifier.code)
        assertFailure(AuthException.InvalidCode) { repository.verifyCode(oldChallenge, generator.nextCode) }
    }

    @Test
    fun `disabled notifications during resend invalidate previous code`() = runTest {
        val oldChallenge = repository.signIn("User", "news")
        notifier.available = false

        assertFailure(AuthException.NotificationsUnavailable) { repository.resendCode(oldChallenge) }
        assertNull(notifier.code)
        assertFailure(AuthException.InvalidCode) { repository.verifyCode(oldChallenge, generator.nextCode) }
    }

    private suspend fun assertFailure(expected: AuthException, action: suspend () -> Unit) {
        try {
            action()
            fail("Expected authentication failure")
        } catch (actual: AuthException) {
            assertEquals(expected, actual)
        }
    }

    private class FakeNotifier : AuthCodeNotifier {
        var available = true
        var code: String? = null
        var shownCount = 0

        override fun show(code: String): Boolean {
            if (!available) return false
            this.code = code
            shownCount++
            return true
        }

        override fun clear() {
            code = null
        }
    }

    private class FakeGenerator : AuthCodeGenerator {
        var nextCode = "1234"
        var excludedCode: String? = null
        var generatedCount = 0

        override fun generate(excluding: String?): String {
            excludedCode = excluding
            generatedCount++
            return nextCode
        }
    }

    private class FakeClock : AuthChallengeClock {
        var timeMillis = 1_000L

        override fun nowMillis(): Long = timeMillis
    }
}
