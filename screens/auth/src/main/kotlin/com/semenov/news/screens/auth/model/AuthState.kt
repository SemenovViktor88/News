package com.semenov.news.screens.auth.model

import com.semenov.news.core.ui.mvi.domain.model.ErrorState
import com.semenov.news.core.ui.mvi.domain.model.UiState

enum class AuthStage { Login, Code }

enum class AuthError { InvalidUserName, InvalidCredentials, InvalidCode, Unavailable }

data class AuthState(
    val stage: AuthStage = AuthStage.Login,
    val userName: String = "",
    val password: String = "",
    val code: String = "",
    val isSubmitting: Boolean = false,
    val error: AuthError? = null,
) : UiState {
    override val hasContent: Boolean = true
    override val initialErrorState: ErrorState? = null

    // MviViewModel logs state transitions. Credentials must never be included.
    override fun toString(): String =
        "AuthState(stage=$stage, userName=[redacted], password=[redacted], code=[redacted], isSubmitting=$isSubmitting, error=$error)"
}
