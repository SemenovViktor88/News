package com.semenov.news.screens.auth

import androidx.lifecycle.viewModelScope
import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import com.semenov.news.features.auth.domain.model.AuthChallenge
import com.semenov.news.features.auth.domain.model.AuthException
import com.semenov.news.features.auth.domain.model.AuthUser
import com.semenov.news.features.auth.domain.repository.AuthRepository
import com.semenov.news.features.auth.domain.session.SessionManager
import com.semenov.news.screens.auth.model.AuthError
import com.semenov.news.screens.auth.model.AuthIntent
import com.semenov.news.screens.auth.model.AuthStage
import com.semenov.news.screens.auth.reducer.AuthReducer
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()
    private val session = FakeSessionManager()
    private val haptics = FakeHapticManager()
    private val logger = RecordingLogger()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = AuthViewModel(repository, session, AuthReducer(), haptics, logger)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `correct first factor opens code stage without persisting a user`() = runTest(dispatcher) {
        credentials()
        viewModel.processIntent(AuthIntent.SignIn)
        advanceUntilIdle()

        assertEquals(AuthStage.Code, viewModel.state.value.stage)
        assertEquals("Viktor", viewModel.state.value.userName)
        assertEquals("", viewModel.state.value.password)
        assertNull(session.user.value)
        assertEquals(0, session.saveCount)
    }

    @Test
    fun `invalid password keeps login and reports rejection`() = runTest(dispatcher) {
        credentials()
        repository.signInFailure = AuthException.InvalidCredentials
        viewModel.processIntent(AuthIntent.SignIn)
        advanceUntilIdle()

        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        assertEquals(AuthError.InvalidCredentials, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSubmitting)
        assertNull(session.user.value)
        assertEquals(AppHapticEffect.Reject, haptics.performed.last())
    }

    @Test
    fun `invalid code does not create a saved session and can be retried`() = runTest(dispatcher) {
        login()
        repository.verifyFailure = AuthException.InvalidCode
        viewModel.processIntent(AuthIntent.CodeChanged("1234"))
        viewModel.processIntent(AuthIntent.VerifyCode)
        advanceUntilIdle()
        assertEquals(AuthError.InvalidCode, viewModel.state.value.error)
        assertEquals(AuthStage.Code, viewModel.state.value.stage)
        assertNull(session.user.value)

        repository.verifyFailure = null
        viewModel.processIntent(AuthIntent.CodeChanged("1111"))
        viewModel.processIntent(AuthIntent.VerifyCode)
        advanceUntilIdle()

        assertEquals(AuthUser("Viktor"), session.user.value)
        assertEquals(1, session.saveCount)
        assertEquals("", viewModel.state.value.code)
        assertNull(viewModel.state.value.error)
        assertEquals(AppHapticEffect.Confirm, haptics.performed.last())
    }

    @Test
    fun `back clears challenge and verification input before returning to login`() = runTest(dispatcher) {
        login()
        viewModel.processIntent(AuthIntent.CodeChanged("1111"))
        viewModel.processIntent(AuthIntent.BackToLogin)
        viewModel.processIntent(AuthIntent.VerifyCode)
        advanceUntilIdle()

        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        assertEquals("Viktor", viewModel.state.value.userName)
        assertEquals("", viewModel.state.value.password)
        assertEquals("", viewModel.state.value.code)
        assertEquals(0, repository.verifyCount)
        assertNull(session.user.value)
    }

    @Test
    fun `username is limited to 32 complete unicode characters`() = runTest(dispatcher) {
        viewModel.processIntent(AuthIntent.UserNameChanged("🙂".repeat(33)))
        advanceUntilIdle()

        assertEquals("🙂".repeat(32), viewModel.state.value.userName)
    }

    @Test
    fun `code accepts only four ascii digits including paste`() = runTest(dispatcher) {
        login()
        viewModel.processIntent(AuthIntent.CodeChanged("1a 12-34５"))
        advanceUntilIdle()

        assertEquals("1123", viewModel.state.value.code)
    }

    @Test
    fun `duplicate submits cannot start another request while one is pending`() = runTest(dispatcher) {
        credentials()
        val gate = CompletableDeferred<Unit>()
        repository.signInGate = gate
        viewModel.processIntent(AuthIntent.SignIn)
        viewModel.processIntent(AuthIntent.SignIn)
        runCurrent()

        assertTrue(viewModel.state.value.isSubmitting)
        assertEquals(1, repository.signInCount)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(AuthStage.Code, viewModel.state.value.stage)
    }

    @Test
    fun `failure to persist session leaves verification open for retry`() = runTest(dispatcher) {
        login()
        session.saveFailure = IOException("disk unavailable")
        viewModel.processIntent(AuthIntent.CodeChanged("1111"))
        viewModel.processIntent(AuthIntent.VerifyCode)
        advanceUntilIdle()

        assertNull(session.user.value)
        assertEquals(AuthError.Unavailable, viewModel.state.value.error)
        assertEquals(AuthStage.Code, viewModel.state.value.stage)
        assertFalse(viewModel.state.value.isSubmitting)
        session.saveFailure = null
        viewModel.processIntent(AuthIntent.VerifyCode)
        advanceUntilIdle()
        assertEquals(AuthUser("Viktor"), session.user.value)
    }

    @Test
    fun `MVI logs do not contain password or verification code`() = runTest(dispatcher) {
        credentials(password = "secret-password")
        viewModel.processIntent(AuthIntent.SignIn)
        advanceUntilIdle()
        viewModel.processIntent(AuthIntent.CodeChanged("9876"))
        viewModel.processIntent(AuthIntent.VerifyCode)
        advanceUntilIdle()

        assertFalse(logger.messages.any { "secret-password" in it || "9876" in it })
        assertTrue(logger.messages.any { "[redacted]" in it })
    }

    private fun credentials(password: String = "news") {
        viewModel.processIntent(AuthIntent.UserNameChanged(" Viktor "))
        viewModel.processIntent(AuthIntent.PasswordChanged(password))
        dispatcher.scheduler.advanceUntilIdle()
    }

    private fun login() {
        credentials()
        viewModel.processIntent(AuthIntent.SignIn)
        dispatcher.scheduler.advanceUntilIdle()
    }

    private class FakeAuthRepository : AuthRepository {
        var signInFailure: Exception? = null
        var verifyFailure: Exception? = null
        var signInGate: CompletableDeferred<Unit>? = null
        var signInCount = 0
        var verifyCount = 0

        override suspend fun signIn(userName: String, password: String): AuthChallenge {
            signInCount++
            signInGate?.await()
            signInFailure?.let { throw it }
            return AuthChallenge(userName.trim())
        }

        override suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthUser {
            verifyCount++
            verifyFailure?.let { throw it }
            return AuthUser(challenge.userName)
        }
    }

    private class FakeSessionManager : SessionManager {
        override val user = MutableStateFlow<AuthUser?>(null)
        var saveCount = 0
        var saveFailure: Exception? = null
        override suspend fun saveUser(user: AuthUser) {
            saveFailure?.let { throw it }
            saveCount++
            this.user.value = user
        }
        override suspend fun logout() { user.value = null }
    }

    private class FakeHapticManager : HapticEffectManager {
        override val effects = emptyFlow<AppHapticEffect>()
        val performed = mutableListOf<AppHapticEffect>()
        override fun performBlocking(effect: AppHapticEffect) { performed += effect }
        override suspend fun perform(effect: AppHapticEffect) { performed += effect }
    }

    private class RecordingLogger : AppLogger {
        val messages = mutableListOf<String>()
        override fun log(tag: String, message: String, t: Throwable?) { messages += message }
    }
}
