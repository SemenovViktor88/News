package com.semenov.news.screens.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semenov.news.core.ui.mvi.presentation.BaseScreen
import com.semenov.news.screens.settings.model.SettingsIntent
import com.semenov.news.screens.settings.model.SettingsState

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        onInitialize = viewModel::initialize,
        onIntent = viewModel::processIntent,
    )
}

@Composable
internal fun SettingsContent(
    state: SettingsState,
    onInitialize: () -> Unit,
    onIntent: (SettingsIntent) -> Unit,
) {
    BaseScreen(
        state = state,
        init = onInitialize,
    ) {
        SettingsContent(
            darkTheme = state.darkTheme ?: isSystemInDarkTheme(),
            hapticsEnabled = state.hapticsEnabled,
            onDarkThemeChange = { onIntent(SettingsIntent.DarkThemeChanged(it)) },
            onHapticsChange = { onIntent(SettingsIntent.HapticsChanged(it)) },
            onLampToggle = { onIntent(SettingsIntent.LampPulled(it)) },
            userName = state.userName,
            isLoggingOut = state.isLoggingOut,
            logoutError = state.logoutError,
            onLogout = { onIntent(SettingsIntent.LogoutClicked) },
        )
    }
}
