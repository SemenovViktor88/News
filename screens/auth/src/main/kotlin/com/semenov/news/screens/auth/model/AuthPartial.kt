package com.semenov.news.screens.auth.model

import com.semenov.news.core.ui.mvi.domain.model.UiPartial

sealed interface AuthPartial : UiPartial {
    data class UserNameChanged(val value: String) : AuthPartial {
        override fun toString() = "UserNameChanged([redacted])"
    }
    data class PasswordChanged(val value: String) : AuthPartial {
        override fun toString() = "PasswordChanged([redacted])"
    }
    data class CodeChanged(val value: String) : AuthPartial {
        override fun toString() = "CodeChanged([redacted])"
    }
    data class CodeRequested(val userName: String) : AuthPartial {
        override fun toString() = "CodeRequested([redacted])"
    }
    data class Failed(val error: AuthError) : AuthPartial
    data object Submitting : AuthPartial
    data object BackToLogin : AuthPartial
    data object Authenticated : AuthPartial
}
