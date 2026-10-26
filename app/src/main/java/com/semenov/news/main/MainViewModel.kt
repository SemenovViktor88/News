package com.semenov.news.main

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.core.ui.navigation.domain.NavigationManager
import com.semenov.news.core.ui.navigation.domain.AppFlow
import com.semenov.news.features.auth.domain.session.SessionManager
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
    private val settingsStore: AppSettingsStore,
    private val sessionManager: SessionManager,
    logger: AppLogger,
) : MviViewModel<MainState, MainPartial, MainIntent, MainEffect>(
        initialState = settingsStore.read().let { settings ->
            MainState(
                darkTheme = settings.darkTheme,
                flow = if (sessionManager.user.value == null) AppFlow.Auth else AppFlow.Regular,
            )
        },
        logger = logger,
    ) {
    override suspend fun processInit() {
        launch {
            sessionManager.user.collect { user ->
                val flow = if (user == null) AppFlow.Auth else AppFlow.Regular
                if (flow != currentState.flow) {
                    navigationManager.reset()
                    sendPartial(MainPartial.FlowChanged(flow))
                }
            }
        }
        launch {
            navigationManager.activeDestination.collect { destination ->
                sendPartial(MainPartial.DestinationChanged(destination))
            }
        }
        settingsStore.settings.collect { settings ->
            sendPartial(MainPartial.ThemeChanged(settings.darkTheme))
        }
    }

    override suspend fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.DestinationSelected -> {
                if (currentState.flow == AppFlow.Regular) {
                    navigationManager.navigate(intent.destination)
                }
            }
        }
    }

    override suspend fun reduceWithPartial(
        partial: MainPartial,
        old: MainState,
    ): MainState = reducer.reduce(partial, old)
}
