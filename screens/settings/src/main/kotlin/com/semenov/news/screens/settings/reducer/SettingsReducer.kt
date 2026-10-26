package com.semenov.news.screens.settings.reducer

import com.semenov.news.core.ui.mvi.domain.Reducer
import com.semenov.news.screens.settings.model.SettingsPartial
import com.semenov.news.screens.settings.model.SettingsState
import javax.inject.Inject

class SettingsReducer @Inject constructor() : Reducer<SettingsState, SettingsPartial> {
    override suspend fun reduce(
        partial: SettingsPartial,
        old: SettingsState,
    ): SettingsState =
        when (partial) {
            is SettingsPartial.PreferencesChanged ->
                old.copy(
                    darkTheme = partial.darkTheme,
                    hapticsEnabled = partial.hapticsEnabled,
                )
        }
}
