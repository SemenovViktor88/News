package com.semenov.news.main.reducer

import com.semenov.news.core.ui.mvi.domain.Reducer
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
        }
}