package com.semenov.news.main.models

import com.semenov.news.core.ui.mvi.domain.model.ErrorState
import com.semenov.news.core.ui.mvi.domain.model.UiEffect
import com.semenov.news.core.ui.mvi.domain.model.UiIntent
import com.semenov.news.core.ui.mvi.domain.model.UiPartial
import com.semenov.news.core.ui.mvi.domain.model.UiState
import com.semenov.news.core.ui.navigation.domain.AppDestination
import com.semenov.news.core.ui.navigation.domain.AppFlow
import com.semenov.news.core.ui.interactions.domain.model.dialog.DialogDestination
import com.semenov.news.core.ui.interactions.domain.model.modal.ModalDestination

data class MainState(
    val activeDestination: AppDestination = AppDestination.Home,
    val darkTheme: Boolean? = null,
    val flow: AppFlow = AppFlow.Auth,
    val dialog: DialogDestination? = null,
    val modal: ModalDestination? = null,
) : UiState {
    override val hasContent: Boolean = true
    override val initialErrorState: ErrorState? = null
}

sealed interface MainIntent : UiIntent {
    data class DestinationSelected(val destination: AppDestination) : MainIntent

    data object DismissDialog : MainIntent

    data class DismissModal(val id: String) : MainIntent
}

sealed interface MainPartial : UiPartial {
    data class DestinationChanged(val destination: AppDestination) : MainPartial

    data class ThemeChanged(val darkTheme: Boolean?) : MainPartial

    data class FlowChanged(val flow: AppFlow) : MainPartial

    data class DialogUpdated(val dialog: DialogDestination?) : MainPartial

    data class ModalUpdated(val modal: ModalDestination?) : MainPartial
}

sealed interface MainEffect : UiEffect
