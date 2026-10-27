package com.semenov.news.screens.auth

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import com.semenov.news.core.ui.interactions.domain.manager.ModalManager
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import com.semenov.news.core.ui.interactions.domain.model.modal.ModalDestination
import com.semenov.news.core.ui.interactions.domain.model.modal.VerificationCodeError
import com.semenov.news.core.ui.interactions.domain.model.modal.VerificationCodeState
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.features.auth.domain.model.AuthChallenge
import com.semenov.news.features.auth.domain.model.AuthException
import com.semenov.news.features.auth.domain.model.AuthUser
import com.semenov.news.features.auth.domain.repository.AuthRepository
import com.semenov.news.features.auth.domain.session.SessionManager
import com.semenov.news.screens.auth.model.AuthEffect
import com.semenov.news.screens.auth.model.AuthError
import com.semenov.news.screens.auth.model.AuthIntent
import com.semenov.news.screens.auth.model.AuthPartial
import com.semenov.news.screens.auth.model.AuthStage
import com.semenov.news.screens.auth.model.AuthState
import com.semenov.news.screens.auth.reducer.AuthReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val authReducer: AuthReducer,
    private val hapticEffectManager: HapticEffectManager,
    private val modalManager: ModalManager,
    logger: AppLogger,
) : MviViewModel<AuthState, AuthPartial, AuthIntent, AuthEffect>(AuthState(), logger) {
    private var verification: PendingVerification? = null
    private var committingSession = false

    override suspend fun processInit() {
        // The activity may have finished while a sheet was open. A new auth
        // owner starts from its own challenge instead of retaining old callbacks.
        if (verification == null && modalManager.currentModal.value is ModalDestination.VerificationCode) {
            modalManager.dismissModal()
        }
        modalManager.currentModal.bind { modal ->
            val pending = verification
            if (pending != null && modal?.id != pending.id && !committingSession) {
                abandonVerification(pending)
            }
        }
    }

    override suspend fun handleIntent(intent: AuthIntent) {
        // A dismissed/replaced sheet can still have queued callbacks. They cannot
        // update another challenge, even if the username happens to be identical.
        when (intent) {
            is AuthIntent.VerificationAnimationFinished -> {
                completeVerification(intent.modalId)
                return
            }
            is AuthIntent.ModalDismissed -> {
                verification?.takeIf { it.id == intent.modalId && !committingSession }?.let {
                    abandonVerification(it)
                }
                return
            }
            else -> Unit
        }
        if (currentState.isSubmitting || currentState.isVerified) return
        when (intent) {
            is AuthIntent.UserNameChanged -> {
                if (currentState.stage == AuthStage.Login) {
                    sendPartial(AuthPartial.UserNameChanged(intent.value.takeCodePoints(MAX_USER_NAME_LENGTH)))
                }
            }
            is AuthIntent.PasswordChanged -> {
                if (currentState.stage == AuthStage.Login) sendPartial(AuthPartial.PasswordChanged(intent.value))
            }
            is AuthIntent.CodeChanged -> {
                val pending = currentVerification(intent.modalId) ?: return
                val code = intent.value.filter { it in '0'..'9' }.take(CODE_LENGTH)
                if (code == currentState.code) return
                sendPartial(AuthPartial.CodeChanged(code))
                if (code.length == CODE_LENGTH) verifyCode(pending)
            }
            AuthIntent.SignIn -> signIn()
            AuthIntent.NotificationPermissionDenied -> {
                if (currentState.stage == AuthStage.Login) {
                    reportFailure(AuthException.NotificationsUnavailable)
                }
            }
            is AuthIntent.VerifyCode -> currentVerification(intent.modalId)?.let { verifyCode(it) }
            is AuthIntent.ResendCode -> currentVerification(intent.modalId)?.let { resendCode(it) }
            AuthIntent.BackToLogin -> {
                verification?.let { pending ->
                    abandonVerification(pending)
                    if (modalManager.currentModal.value?.id == pending.id) modalManager.dismissModal()
                    hapticEffectManager.perform(AppHapticEffect.Click)
                }
            }
            is AuthIntent.ModalDismissed -> Unit
            is AuthIntent.VerificationAnimationFinished -> Unit
        }
    }

    private suspend fun signIn() {
        if (currentState.stage != AuthStage.Login) return
        val input = currentState
        sendPartial(AuthPartial.Submitting)
        hapticEffectManager.perform(AppHapticEffect.Click)
        try {
            val challenge = authRepository.signIn(input.userName, input.password)
            val pending = PendingVerification(challenge)
            verification = pending
            sendPartial(AuthPartial.CodeRequested(challenge.userName))
            modalManager.showModal(
                ModalDestination.VerificationCode(
                    userName = challenge.userName,
                    state = pending.state.asStateFlow(),
                    id = pending.id,
                    onCodeChanged = { processIntent(AuthIntent.CodeChanged(it, pending.id)) },
                    onVerify = { processIntent(AuthIntent.VerifyCode(pending.id)) },
                    onResend = { processIntent(AuthIntent.ResendCode(pending.id)) },
                    onSuccessAnimationFinished = {
                        processIntent(AuthIntent.VerificationAnimationFinished(pending.id))
                    },
                    onDismiss = { processIntent(AuthIntent.ModalDismissed(pending.id)) },
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            reportFailure(error)
        }
    }

    private suspend fun verifyCode(pending: PendingVerification) {
        if (!isCurrent(pending) || currentState.code.length != CODE_LENGTH) return
        val inputCode = currentState.code
        sendPartial(AuthPartial.Submitting)
        hapticEffectManager.perform(AppHapticEffect.Click)
        try {
            // A consumed one-time code must not be verified again if saving the
            // session failed. An explicit retry can reuse this in-memory result.
            val user = pending.verifiedUser ?: authRepository.verifyCode(pending.challenge, inputCode)
            if (!isCurrent(pending)) return
            pending.verifiedUser = user
            sendPartial(AuthPartial.Verified)
            hapticEffectManager.perform(AppHapticEffect.Confirm)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (isCurrent(pending)) reportFailure(error) else abandonVerification(pending)
        }
    }

    private suspend fun resendCode(pending: PendingVerification) {
        if (!isCurrent(pending) || pending.verifiedUser != null) return
        sendPartial(AuthPartial.Submitting)
        hapticEffectManager.perform(AppHapticEffect.Click)
        try {
            val challenge = authRepository.resendCode(pending.challenge)
            if (!isCurrent(pending)) {
                cancelChallenge(challenge)
                return
            }
            pending.challenge = challenge
            pending.verifiedUser = null
            sendPartial(AuthPartial.CodeResent)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (!isCurrent(pending)) return
            if (error == AuthException.NotificationsUnavailable) {
                abandonVerification(pending, failure = error)
                if (modalManager.currentModal.value?.id == pending.id) modalManager.dismissModal()
            } else {
                reportFailure(error)
            }
        }
    }

    private suspend fun completeVerification(modalId: String) {
        val pending = verification?.takeIf {
            it.id == modalId && isCurrent(it) && currentState.isVerified && !committingSession
        } ?: return
        // The animation owns its timing. A successful response stays in memory
        // until this sheet finishes, and each completion can create only one session.
        val user = pending.verifiedUser ?: return
        committingSession = true
        try {
            sessionManager.saveUser(user)
            pending.verifiedUser = null
            verification = null
            sendPartial(AuthPartial.Authenticated)
            if (modalManager.currentModal.value?.id == pending.id) modalManager.dismissModal()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (isCurrent(pending)) reportFailure(error, clearVerifiedUser = false) else abandonVerification(pending)
        } finally {
            committingSession = false
        }
    }

    private fun currentVerification(modalId: String?): PendingVerification? =
        verification?.takeIf {
            currentState.stage == AuthStage.Code &&
                (modalId == null || modalId == it.id) && isCurrent(it)
        }

    private fun isCurrent(pending: PendingVerification): Boolean =
        verification === pending && modalManager.currentModal.value?.id == pending.id

    private suspend fun abandonVerification(pending: PendingVerification, failure: Exception? = null) {
        if (verification !== pending) return
        pending.verifiedUser = null
        verification = null
        pending.state.value = VerificationCodeState()
        sendPartial(AuthPartial.BackToLogin)
        failure?.let { reportFailure(it) }
        cancelChallenge(pending.challenge)
    }

    private suspend fun cancelChallenge(challenge: AuthChallenge) {
        try {
            authRepository.cancelChallenge(challenge)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logError(error, "cancel verification challenge")
        }
    }

    private suspend fun reportFailure(error: Exception, clearVerifiedUser: Boolean = true) {
        if (clearVerifiedUser) verification?.verifiedUser = null
        val authError = when (error) {
            AuthException.InvalidUserName -> AuthError.InvalidUserName
            AuthException.InvalidCredentials -> AuthError.InvalidCredentials
            AuthException.InvalidCode -> AuthError.InvalidCode
            AuthException.NotificationsUnavailable -> AuthError.NotificationsUnavailable
            else -> AuthError.Unavailable
        }
        sendPartial(AuthPartial.Failed(authError))
        hapticEffectManager.perform(AppHapticEffect.Reject)
    }

    override suspend fun reduceWithPartial(partial: AuthPartial, old: AuthState): AuthState =
        authReducer.reduce(partial, old).also { next ->
            verification?.state?.value = VerificationCodeState(
                code = next.code,
                isSubmitting = next.isSubmitting,
                error = when (next.error) {
                    AuthError.InvalidCode -> VerificationCodeError.InvalidCode
                    AuthError.Unavailable -> VerificationCodeError.Unavailable
                    else -> null
                },
                isVerified = next.isVerified,
                resendCount = next.resendCount,
                canResend = verification?.verifiedUser == null,
            )
        }

    private class PendingVerification(var challenge: AuthChallenge) {
        val id = UUID.randomUUID().toString()
        val state = MutableStateFlow(VerificationCodeState())
        var verifiedUser: AuthUser? = null
    }
}

private fun String.takeCodePoints(count: Int): String =
    if (codePointCount(0, length) <= count) this else substring(0, offsetByCodePoints(0, count))

private const val MAX_USER_NAME_LENGTH = 32
private const val CODE_LENGTH = 4
