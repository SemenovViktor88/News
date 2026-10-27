package com.semenov.news.screens.auth.model

import com.semenov.news.core.ui.mvi.domain.model.UiIntent

sealed interface AuthIntent : UiIntent {
    data class UserNameChanged(val value: String) : AuthIntent {
        override fun toString() = "UserNameChanged([redacted])"
    }
    data class PasswordChanged(val value: String) : AuthIntent {
        override fun toString() = "PasswordChanged([redacted])"
    }
    data class CodeChanged(val value: String, val modalId: String? = null) : AuthIntent {
        override fun toString() = "CodeChanged([redacted])"
    }
    data object SignIn : AuthIntent
    data object NotificationPermissionDenied : AuthIntent
    data class VerifyCode(val modalId: String? = null) : AuthIntent
    data class ResendCode(val modalId: String? = null) : AuthIntent
    data class ModalDismissed(val modalId: String) : AuthIntent
    data class VerificationAnimationFinished(val modalId: String) : AuthIntent
    data object BackToLogin : AuthIntent
}
