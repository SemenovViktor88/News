package com.semenov.news.screens.auth

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.features.auth.domain.model.AuthChallenge
import com.semenov.news.features.auth.domain.model.AuthException
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
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val authReducer: AuthReducer,
    private val hapticEffectManager: HapticEffectManager,
    logger: AppLogger,
) : MviViewModel<AuthState, AuthPartial, AuthIntent, AuthEffect>(AuthState(), logger) {
    private var challenge: AuthChallenge? = null

    override suspend fun processInit() = Unit

    override suspend fun handleIntent(intent: AuthIntent) {
        if (currentState.isSubmitting) return
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
                if (currentState.stage == AuthStage.Code) {
                    sendPartial(AuthPartial.CodeChanged(intent.value.filter { it in '0'..'9' }.take(CODE_LENGTH)))
                }
            }
            AuthIntent.SignIn -> signIn()
            AuthIntent.VerifyCode -> verifyCode()
            AuthIntent.BackToLogin -> {
                challenge = null
                hapticEffectManager.perform(AppHapticEffect.Click)
                sendPartial(AuthPartial.BackToLogin)
            }
        }
    }

    private suspend fun signIn() {
        if (currentState.stage != AuthStage.Login) return
        val input = currentState
        submit {
            val result = authRepository.signIn(input.userName, input.password)
            challenge = result
            sendPartial(AuthPartial.CodeRequested(result.userName))
        }
    }

    private suspend fun verifyCode() {
        if (currentState.stage != AuthStage.Code) return
        val pendingChallenge = challenge ?: return
        val inputCode = currentState.code
        submit {
            val user = authRepository.verifyCode(pendingChallenge, inputCode)
            sessionManager.saveUser(user)
            challenge = null
            sendPartial(AuthPartial.Authenticated)
            hapticEffectManager.perform(AppHapticEffect.Confirm)
        }
    }

    private suspend fun submit(block: suspend () -> Unit) {
        sendPartial(AuthPartial.Submitting)
        hapticEffectManager.perform(AppHapticEffect.Click)
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val authError = when (error) {
                AuthException.InvalidUserName -> AuthError.InvalidUserName
                AuthException.InvalidCredentials -> AuthError.InvalidCredentials
                AuthException.InvalidCode -> AuthError.InvalidCode
                else -> AuthError.Unavailable
            }
            sendPartial(AuthPartial.Failed(authError))
            hapticEffectManager.perform(AppHapticEffect.Reject)
        }
    }

    override suspend fun reduceWithPartial(partial: AuthPartial, old: AuthState): AuthState =
        authReducer.reduce(partial, old)
}

private fun String.takeCodePoints(count: Int): String =
    if (codePointCount(0, length) <= count) this else substring(0, offsetByCodePoints(0, count))

private const val MAX_USER_NAME_LENGTH = 32
private const val CODE_LENGTH = 4
