package com.semenov.news.features.auth.domain.model

sealed class AuthException(message: String) : Exception(message) {
    data object InvalidUserName : AuthException("Enter a user name of 1 to 32 characters")

    data object InvalidCredentials : AuthException("Incorrect user name or password")

    data object InvalidCode : AuthException("Incorrect verification code")

    data object NotificationsUnavailable : AuthException("Enable notifications to receive a verification code")
}
