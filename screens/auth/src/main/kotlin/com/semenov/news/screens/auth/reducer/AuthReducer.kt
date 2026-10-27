package com.semenov.news.screens.auth.reducer

import com.semenov.news.core.ui.mvi.domain.Reducer
import com.semenov.news.screens.auth.model.AuthPartial
import com.semenov.news.screens.auth.model.AuthStage
import com.semenov.news.screens.auth.model.AuthState
import javax.inject.Inject

class AuthReducer @Inject constructor() : Reducer<AuthState, AuthPartial> {
    override suspend fun reduce(partial: AuthPartial, old: AuthState): AuthState =
        when (partial) {
            is AuthPartial.UserNameChanged -> old.copy(userName = partial.value, error = null)
            is AuthPartial.PasswordChanged -> old.copy(password = partial.value, error = null)
            is AuthPartial.CodeChanged -> old.copy(code = partial.value, error = null)
            is AuthPartial.CodeRequested -> old.copy(
                stage = AuthStage.Code,
                userName = partial.userName,
                password = "",
                code = "",
                isSubmitting = false,
                error = null,
                isVerified = false,
                resendCount = 0,
            )
            is AuthPartial.Failed -> old.copy(isSubmitting = false, error = partial.error, isVerified = false)
            AuthPartial.Verified -> old.copy(isSubmitting = false, error = null, isVerified = true)
            AuthPartial.CodeResent -> old.copy(code = "", error = null, isSubmitting = false, resendCount = old.resendCount + 1)
            AuthPartial.Submitting -> old.copy(isSubmitting = true, error = null)
            AuthPartial.BackToLogin -> AuthState(userName = old.userName)
            AuthPartial.Authenticated -> old.copy(password = "", code = "", isSubmitting = false, error = null)
        }
}
