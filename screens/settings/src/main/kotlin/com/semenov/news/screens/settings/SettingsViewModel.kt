package com.semenov.news.screens.settings

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.settings.AppSettings
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import com.semenov.news.screens.settings.model.SettingsEffect
import com.semenov.news.screens.settings.model.SettingsIntent
import com.semenov.news.screens.settings.model.SettingsPartial
import com.semenov.news.screens.settings.model.SettingsState
import com.semenov.news.screens.settings.reducer.SettingsReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
    private val settingsReducer: SettingsReducer,
    private val hapticEffectManager: HapticEffectManager,
    logger: AppLogger,
) : MviViewModel<SettingsState, SettingsPartial, SettingsIntent, SettingsEffect>(
        initialState = settingsStore.read().toState(),
        logger = logger,
    ) {
    override suspend fun processInit() {
        settingsStore.settings.collect { settings ->
            sendPartial(
                SettingsPartial.PreferencesChanged(
                    darkTheme = settings.darkTheme,
                    hapticsEnabled = settings.hapticsEnabled,
                ),
            )
        }
    }

    override suspend fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.DarkThemeChanged -> {
                if (settingsStore.read().darkTheme != intent.enabled) {
                    hapticEffectManager.perform(
                        if (intent.enabled) AppHapticEffect.ToggleOn else AppHapticEffect.ToggleOff,
                    )
                    settingsStore.setDarkTheme(intent.enabled)
                }
            }
            is SettingsIntent.LampPulled -> {
                hapticEffectManager.perform(AppHapticEffect.LongPress)
                settingsStore.setDarkTheme(intent.darkTheme)
            }
            is SettingsIntent.HapticsChanged -> {
                if (settingsStore.read().hapticsEnabled != intent.enabled) {
                    if (intent.enabled) {
                        settingsStore.setHapticsEnabled(true)
                        hapticEffectManager.perform(AppHapticEffect.ToggleOn)
                    } else {
                        hapticEffectManager.perform(AppHapticEffect.ToggleOff)
                        settingsStore.setHapticsEnabled(false)
                    }
                }
            }
        }
    }

    override suspend fun reduceWithPartial(
        partial: SettingsPartial,
        old: SettingsState,
    ): SettingsState = settingsReducer.reduce(partial, old)
}

private fun AppSettings.toState() =
    SettingsState(
        darkTheme = darkTheme,
        hapticsEnabled = hapticsEnabled,
    )
