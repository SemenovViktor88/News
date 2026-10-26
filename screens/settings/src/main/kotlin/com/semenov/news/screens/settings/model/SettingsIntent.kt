package com.semenov.news.screens.settings.model

import com.semenov.news.core.ui.mvi.domain.model.UiIntent

sealed interface SettingsIntent : UiIntent {
    data class DarkThemeChanged(val enabled: Boolean) : SettingsIntent

    data class LampPulled(val darkTheme: Boolean) : SettingsIntent

    data class HapticsChanged(val enabled: Boolean) : SettingsIntent

    data object LogoutClicked : SettingsIntent
}
