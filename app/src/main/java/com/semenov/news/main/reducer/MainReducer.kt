package com.semenov.news.main.reducer

import com.semenov.news.core.ui.mvi.domain.Reducer
import com.semenov.news.core.ui.navigation.domain.AppDestination
import com.semenov.news.main.models.MainPartial
import com.semenov.news.main.models.MainState
import javax.inject.Inject

class MainReducer @Inject constructor() : Reducer<MainState, MainPartial> {
    override suspend fun reduce(
        partial: MainPartial,
        old: MainState,
    ): MainState =
        when (partial) {
            is MainPartial.DestinationChanged ->
                old.copy(activeDestination = partial.destination)
            is MainPartial.ThemeChanged ->
                old.copy(darkTheme = partial.darkTheme)
            is MainPartial.FlowChanged ->
                old.copy(flow = partial.flow, activeDestination = AppDestination.Home)
            is MainPartial.DialogUpdated ->
                old.copy(dialog = partial.dialog)
            is MainPartial.ModalUpdated ->
                old.copy(modal = partial.modal)
        }
}
