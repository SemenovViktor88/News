package com.semenov.news.screens.settings.model

import com.semenov.news.core.ui.mvi.domain.model.ErrorState
import com.semenov.news.core.ui.mvi.domain.model.UiState

data class SettingsState(
    val darkTheme: Boolean? = null,
    val hapticsEnabled: Boolean = true,
) : UiState {
    override val hasContent: Boolean = true
    override val initialErrorState: ErrorState? = null
}
