package com.semenov.news.main.models

import com.semenov.news.core.ui.mvi.domain.model.ErrorState
import com.semenov.news.core.ui.mvi.domain.model.UiEffect
import com.semenov.news.core.ui.mvi.domain.model.UiIntent
import com.semenov.news.core.ui.mvi.domain.model.UiPartial
import com.semenov.news.core.ui.mvi.domain.model.UiState
import com.semenov.news.core.ui.navigation.domain.AppDestination

data class MainState(
    val activeDestination: AppDestination = AppDestination.Home,
) : UiState {
    override val hasContent: Boolean = true
    override val initialErrorState: ErrorState? = null
}

sealed interface MainIntent : UiIntent {
    data class DestinationSelected(val destination: AppDestination) : MainIntent
}

sealed interface MainPartial : UiPartial {
    data class DestinationChanged(val destination: AppDestination) : MainPartial
}

sealed interface MainEffect : UiEffect
