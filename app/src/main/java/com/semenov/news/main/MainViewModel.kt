package com.semenov.news.main

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.core.ui.navigation.domain.NavigationManager
import com.semenov.news.main.models.MainEffect
import com.semenov.news.main.models.MainIntent
import com.semenov.news.main.models.MainPartial
import com.semenov.news.main.models.MainState
import com.semenov.news.main.reducer.MainReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val reducer: MainReducer,
    logger: AppLogger,
) : MviViewModel<MainState, MainPartial, MainIntent, MainEffect>(
        initialState = MainState(),
        logger = logger,
    ) {
    override suspend fun processInit() {
        navigationManager.activeDestination.collect { destination ->
            sendPartial(MainPartial.DestinationChanged(destination))
        }
    }

    override suspend fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.DestinationSelected -> navigationManager.navigate(intent.destination)
        }
    }

    override suspend fun reduceWithPartial(
        partial: MainPartial,
        old: MainState,
    ): MainState = reducer.reduce(partial, old)
}
