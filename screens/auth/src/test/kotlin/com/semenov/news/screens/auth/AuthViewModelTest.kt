package com.semenov.news.screens.auth

import androidx.lifecycle.viewModelScope
import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import com.semenov.news.core.ui.interactions.domain.manager.ModalManager
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import com.semenov.news.core.ui.interactions.domain.model.modal.ModalDestination
import com.semenov.news.core.ui.interactions.domain.model.modal.VerificationCodeError
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    private val modals = FakeModalManager()
    private val logger = RecordingLogger()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = newViewModel().also { it.initialize() }
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `correct first factor opens verification modal without persisting a user`() = runTest(dispatcher) {
        login()

        assertEquals(AuthStage.Code, viewModel.state.value.stage)
        assertEquals("Viktor", modal().userName)
        assertEquals("", viewModel.state.value.password)
        assertEquals("", modal().state.value.code)
        assertNull(session.user.value)
        assertEquals(0, session.saveCount)
    }

    @Test
    fun `invalid password keeps login and reports rejection without opening a modal`() = runTest(dispatcher) {
        credentials(password = "wrong")
        viewModel.processIntent(AuthIntent.SignIn)
        advanceUntilIdle()

        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        assertEquals(AuthError.InvalidCredentials, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSubmitting)
        assertNull(modals.currentModal.value)
        assertNull(session.user.value)
        assertEquals(AppHapticEffect.Reject, haptics.performed.last())
    }

    @Test
    fun `denying notification permission does not request a code`() = runTest(dispatcher) {
        credentials()
        viewModel.processIntent(AuthIntent.NotificationPermissionDenied)
        advanceUntilIdle()

        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        assertEquals(AuthError.NotificationsUnavailable, viewModel.state.value.error)
        assertEquals(0, repository.signInCount)
        assertNull(modals.currentModal.value)
        assertNull(session.user.value)
    }

    @Test
    fun `disabled notification channel reports an actionable login error`() = runTest(dispatcher) {
        repository.signInFailure = AuthException.NotificationsUnavailable
        login()

        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        assertEquals(AuthError.NotificationsUnavailable, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSubmitting)
        assertNull(modals.currentModal.value)
        assertNull(session.user.value)
    }

    @Test
    fun `four digits verify but session waits for the completed success animation`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        sheet.onCodeChanged("111")
        runCurrent()
        assertEquals(0, repository.verifyCount)

        sheet.onCodeChanged("4826")
        runCurrent()
        assertEquals(1, repository.verifyCount)
        assertTrue(sheet.state.value.isVerified)
        assertNull(session.user.value)
        advanceTimeBy(10_000)
        runCurrent()
        assertNull(session.user.value)
        assertEquals(0, session.saveCount)
        sheet.onSuccessAnimationFinished()
        runCurrent()

        assertEquals(AuthUser("Viktor"), session.user.value)
        assertEquals(1, session.saveCount)
        assertEquals("", viewModel.state.value.code)
        assertNull(modals.currentModal.value)
        assertEquals(AppHapticEffect.Confirm, haptics.performed.last())
    }

    @Test
    fun `invalid code remains in the sheet and edits clear the error before retry`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        sheet.onCodeChanged("1234")
        advanceUntilIdle()
        assertEquals(VerificationCodeError.InvalidCode, sheet.state.value.error)
        assertEquals(AuthStage.Code, viewModel.state.value.stage)
        assertNull(session.user.value)

        sheet.onCodeChanged("111")
        advanceUntilIdle()
        assertNull(sheet.state.value.error)
        sheet.onCodeChanged("4826")
        advanceUntilIdle()
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertEquals(AuthUser("Viktor"), session.user.value)
        assertEquals(2, repository.verifyCount)
    }

    @Test
    fun `repeated input and verify callbacks do not create duplicate requests`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        val gate = CompletableDeferred<Unit>()
        repository.verifyGate = gate
        sheet.onCodeChanged("4826")
        sheet.onCodeChanged("4826")
        sheet.onVerify()
        runCurrent()
        assertEquals(1, repository.verifyCount)
        assertTrue(sheet.state.value.isSubmitting)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(0, session.saveCount)
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertEquals(1, repository.verifyCount)
        assertEquals(1, session.saveCount)
    }

    @Test
    fun `early wrong and mismatched animation callbacks cannot create a session`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()
        sheet.onCodeChanged("1234")
        advanceUntilIdle()
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertNull(session.user.value)

        val gate = CompletableDeferred<Unit>()
        repository.verifyGate = gate
        sheet.onCodeChanged("4826")
        runCurrent()
        sheet.onSuccessAnimationFinished()
        runCurrent()
        assertTrue(sheet.state.value.isSubmitting)
        assertEquals(0, session.saveCount)

        gate.complete(Unit)
        advanceUntilIdle()
        viewModel.processIntent(AuthIntent.VerificationAnimationFinished("another-modal"))
        advanceUntilIdle()
        assertTrue(sheet.state.value.isVerified)
        assertEquals(0, session.saveCount)
    }

    @Test
    fun `duplicate completion callbacks during and after persistence save only once`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        sheet.onCodeChanged("4826")
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        session.saveGate = gate

        sheet.onSuccessAnimationFinished()
        sheet.onSuccessAnimationFinished()
        runCurrent()
        assertNull(session.user.value)
        gate.complete(Unit)
        advanceUntilIdle()
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()

        assertEquals(AuthUser("Viktor"), session.user.value)
        assertEquals(1, session.saveCount)
    }

    @Test
    fun `a replaced successful sheet cannot complete the new verified challenge`() = runTest(dispatcher) {
        login()
        val old = modal()
        old.onCodeChanged("4826")
        advanceUntilIdle()
        assertTrue(old.state.value.isVerified)
        modals.dismissModal()
        advanceUntilIdle()
        login()
        val fresh = modal()
        fresh.onCodeChanged("4826")
        advanceUntilIdle()

        old.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertNull(session.user.value)
        assertTrue(fresh.state.value.isVerified)
        assertEquals(0, session.saveCount)

        fresh.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertEquals(AuthUser("Viktor"), session.user.value)
        assertEquals(1, session.saveCount)
    }

    @Test
    fun `dismiss clears challenge and sensitive input and returns to login`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        sheet.onCodeChanged("123")
        advanceUntilIdle()
        modals.dismissModal()
        sheet.onDismiss?.invoke()
        advanceUntilIdle()
        sheet.onCodeChanged("4826")
        sheet.onVerify()
        advanceUntilIdle()

        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        assertEquals("Viktor", viewModel.state.value.userName)
        assertEquals("", viewModel.state.value.password)
        assertEquals("", viewModel.state.value.code)
        assertEquals("", sheet.state.value.code)
        assertEquals(0, repository.verifyCount)
        assertEquals(1, repository.cancelled.size)
        assertNull(session.user.value)
    }

    @Test
    fun `callbacks from dismissed sheet cannot change a fresh challenge`() = runTest(dispatcher) {
        login()
        val old = modal()
        modals.dismissModal()
        advanceUntilIdle()
        credentials()
        viewModel.processIntent(AuthIntent.SignIn)
        advanceUntilIdle()
        val fresh = modal()
        assertNotEquals(old.id, fresh.id)

        old.onCodeChanged("4826")
        old.onVerify()
        old.onResend()
        old.onDismiss?.invoke()
        old.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertEquals(AuthStage.Code, viewModel.state.value.stage)
        assertEquals("", fresh.state.value.code)
        assertEquals(0, fresh.state.value.resendCount)
        assertEquals(0, repository.verifyCount)
        assertNull(session.user.value)
    }

    @Test
    fun `external dismissal while request is pending cannot create a session`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        val gate = CompletableDeferred<Unit>()
        repository.verifyGate = gate
        sheet.onCodeChanged("4826")
        runCurrent()
        assertTrue(sheet.state.value.isSubmitting)
        modals.dismissModal()
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        assertFalse(sheet.state.value.isVerified)
        assertNull(session.user.value)
        assertEquals(0, session.saveCount)
    }

    @Test
    fun `external dismissal during success animation cannot create a session`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        sheet.onCodeChanged("4826")
        runCurrent()
        assertTrue(sheet.state.value.isVerified)
        modals.dismissModal()
        advanceUntilIdle()
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()

        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        assertNull(session.user.value)
        assertEquals(0, session.saveCount)
    }

    @Test
    fun `resend replaces the challenge and only the new notification code verifies`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        sheet.onCodeChanged("1234")
        advanceUntilIdle()
        sheet.onResend()
        advanceUntilIdle()
        assertEquals("", sheet.state.value.code)
        assertNull(sheet.state.value.error)
        assertEquals(1, sheet.state.value.resendCount)
        assertEquals(1, repository.resendCount)
        assertEquals(AppHapticEffect.Click, haptics.performed.last())
        sheet.onCodeChanged("4826")
        advanceUntilIdle()
        assertEquals(VerificationCodeError.InvalidCode, sheet.state.value.error)
        sheet.onCodeChanged("7305")
        advanceUntilIdle()
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertEquals(AuthUser("Viktor"), session.user.value)
    }

    @Test
    fun `resend failure keeps the entered code and a duplicate tap cannot issue another request`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        sheet.onCodeChanged("123")
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        repository.resendGate = gate
        repository.resendFailure = IOException("unavailable")
        sheet.onResend()
        sheet.onResend()
        runCurrent()

        assertEquals(1, repository.resendCount)
        assertTrue(sheet.state.value.isSubmitting)
        assertEquals("123", sheet.state.value.code)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals("123", sheet.state.value.code)
        assertEquals(0, sheet.state.value.resendCount)
        assertEquals(VerificationCodeError.Unavailable, sheet.state.value.error)
        assertFalse(sheet.state.value.isSubmitting)

        sheet.onCodeChanged("4826")
        advanceUntilIdle()
        assertTrue(sheet.state.value.isVerified)
    }

    @Test
    fun `resend that finishes after dismissal cancels its new challenge and cannot reopen a sheet`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        val original = repository.activeChallenge!!
        val gate = CompletableDeferred<Unit>()
        repository.resendGate = gate
        sheet.onResend()
        runCurrent()
        modals.dismissModal()
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        assertNull(modals.currentModal.value)
        assertNull(repository.activeChallenge)
        assertEquals(2, repository.cancelled.size)
        assertEquals(original, repository.cancelled.first())
        assertNotEquals(original.id, repository.cancelled.last().id)
        assertNull(session.user.value)
    }

    @Test
    fun `disabled notifications during resend cancel verification and show settings guidance on login`() = runTest(dispatcher) {
        login()
        repository.resendFailure = AuthException.NotificationsUnavailable
        modal().onResend()
        advanceUntilIdle()

        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        assertEquals(AuthError.NotificationsUnavailable, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSubmitting)
        assertNull(modals.currentModal.value)
        assertNull(repository.activeChallenge)
        assertEquals(1, repository.cancelled.size)
    }

    @Test
    fun `resend and code updates are ignored while verifying or showing success`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        val gate = CompletableDeferred<Unit>()
        repository.verifyGate = gate
        sheet.onCodeChanged("4826")
        runCurrent()
        sheet.onCodeChanged("0000")
        sheet.onResend()
        runCurrent()
        assertEquals("4826", sheet.state.value.code)
        assertEquals(0, sheet.state.value.resendCount)
        gate.complete(Unit)
        runCurrent()
        sheet.onCodeChanged("0000")
        sheet.onResend()
        runCurrent()
        assertTrue(sheet.state.value.isVerified)
        assertEquals("4826", sheet.state.value.code)
        advanceUntilIdle()
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertEquals(1, session.saveCount)
    }

    @Test
    fun `username is limited to 32 complete unicode characters`() = runTest(dispatcher) {
        viewModel.processIntent(AuthIntent.UserNameChanged("🙂".repeat(33)))
        advanceUntilIdle()
        assertEquals("🙂".repeat(32), viewModel.state.value.userName)
    }

    @Test
    fun `paste accepts only four ascii digits and triggers a single verification`() = runTest(dispatcher) {
        login()
        modal().onCodeChanged("1a 12-34５")
        advanceUntilIdle()
        assertEquals("1123", viewModel.state.value.code)
        assertEquals(1, repository.verifyCount)
        assertEquals(AuthError.InvalidCode, viewModel.state.value.error)
        assertNull(session.user.value)
    }

    @Test
    fun `duplicate login submits cannot start another request while one is pending`() = runTest(dispatcher) {
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
    fun `persistence failure ignores resend and retries saving the already verified user`() = runTest(dispatcher) {
        login()
        val sheet = modal()
        session.saveFailure = IOException("disk unavailable")
        sheet.onCodeChanged("4826")
        advanceUntilIdle()
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertNull(session.user.value)
        assertEquals(VerificationCodeError.Unavailable, sheet.state.value.error)
        assertFalse(sheet.state.value.isSubmitting)
        assertFalse(sheet.state.value.isVerified)
        assertFalse(sheet.state.value.canResend)

        repository.resendFailure = AuthException.InvalidCode
        sheet.onResend()
        advanceUntilIdle()
        assertEquals(0, repository.resendCount)
        assertEquals(1, repository.verifyCount)
        assertEquals("4826", sheet.state.value.code)
        assertEquals(VerificationCodeError.Unavailable, sheet.state.value.error)
        assertNull(session.user.value)

        session.saveFailure = null
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertEquals(1, repository.verifyCount)
        assertEquals(0, session.saveCount)
        sheet.onVerify()
        advanceUntilIdle()
        assertTrue(sheet.state.value.isVerified)
        assertNull(session.user.value)
        assertEquals(1, repository.verifyCount)
        sheet.onSuccessAnimationFinished()
        advanceUntilIdle()
        assertEquals(AuthUser("Viktor"), session.user.value)
        assertEquals(1, session.saveCount)
        assertEquals(0, repository.resendCount)
    }

    @Test
    fun `a new auth owner removes the stale modal left by a finished activity`() = runTest(dispatcher) {
        login()
        val stale = modal()
        viewModel.viewModelScope.cancel()
        viewModel = newViewModel().also { it.initialize() }
        advanceUntilIdle()
        assertNull(modals.currentModal.value)
        assertEquals(AuthStage.Login, viewModel.state.value.stage)
        stale.onCodeChanged("4826")
        advanceUntilIdle()
        assertNull(session.user.value)
    }

    @Test
    fun `MVI and modal state logs do not contain password or verification code`() = runTest(dispatcher) {
        credentials(password = "secret-password")
        viewModel.processIntent(AuthIntent.SignIn)
        advanceUntilIdle()
        credentials()
        viewModel.processIntent(AuthIntent.SignIn)
        advanceUntilIdle()
        val sheet = modal()
        sheet.onCodeChanged("9876")
        advanceUntilIdle()
        logger.messages += sheet.toString()
        logger.messages += sheet.state.value.toString()

        assertFalse(logger.messages.any { "secret-password" in it || "9876" in it })
        assertTrue(logger.messages.any { "[redacted]" in it })
    }

    private fun newViewModel() = AuthViewModel(repository, session, AuthReducer(), haptics, modals, logger)

    private fun modal() = modals.currentModal.value as ModalDestination.VerificationCode

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
        var signInGate: CompletableDeferred<Unit>? = null
        var verifyGate: CompletableDeferred<Unit>? = null
        var resendGate: CompletableDeferred<Unit>? = null
        var signInFailure: Exception? = null
        var resendFailure: Exception? = null
        var signInCount = 0
        var verifyCount = 0
        var resendCount = 0
        var activeChallenge: AuthChallenge? = null
        private var activeCode: String? = null
        val cancelled = mutableListOf<AuthChallenge>()
        override suspend fun signIn(userName: String, password: String): AuthChallenge {
            signInCount++
            signInGate?.await()
            signInFailure?.let { throw it }
            if (password != "news") throw AuthException.InvalidCredentials
            return AuthChallenge(userName.trim()).also {
                activeChallenge = it
                activeCode = "4826"
            }
        }
        override suspend fun verifyCode(challenge: AuthChallenge, code: String): AuthUser {
            verifyCount++
            verifyGate?.await()
            if (challenge.id != activeChallenge?.id || code != activeCode) throw AuthException.InvalidCode
            activeChallenge = null
            activeCode = null
            return AuthUser(challenge.userName)
        }
        override suspend fun resendCode(challenge: AuthChallenge): AuthChallenge {
            resendCount++
            resendGate?.await()
            resendFailure?.let { throw it }
            return AuthChallenge(challenge.userName).also {
                activeChallenge = it
                activeCode = "7305"
            }
        }
        override suspend fun cancelChallenge(challenge: AuthChallenge) {
            cancelled += challenge
            if (activeChallenge?.id == challenge.id) {
                activeChallenge = null
                activeCode = null
            }
        }
    }

    private class FakeSessionManager : SessionManager {
        override val user = MutableStateFlow<AuthUser?>(null)
        var saveCount = 0
        var saveFailure: Exception? = null
        var saveGate: CompletableDeferred<Unit>? = null
        override suspend fun saveUser(user: AuthUser) {
            saveGate?.await()
            saveFailure?.let { throw it }
            saveCount++
            this.user.value = user
        }
        override suspend fun logout() { user.value = null }
    }

    private class FakeModalManager : ModalManager {
        override val currentModal = MutableStateFlow<ModalDestination?>(null)
        override suspend fun showModal(modal: ModalDestination) { currentModal.value = modal }
        override suspend fun dismissModal() { currentModal.value = null }
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
