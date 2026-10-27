package com.semenov.news.screens.settings.model

import com.semenov.news.core.ui.mvi.domain.model.UiPartial

sealed interface SettingsPartial : UiPartial {
    data class PreferencesChanged(
        val darkTheme: Boolean?,
        val hapticsEnabled: Boolean,
    ) : SettingsPartial

    data class UserChanged(val name: String) : SettingsPartial

    data object LogoutStarted : SettingsPartial

    data object LogoutFinished : SettingsPartial

    data object LogoutFailed : SettingsPartial
}
