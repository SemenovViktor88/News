package com.semenov.news.main

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.core.ui.navigation.domain.NavigationManager
import com.semenov.news.core.ui.navigation.domain.AppFlow
import com.semenov.news.core.ui.interactions.domain.manager.DialogManager
import com.semenov.news.core.ui.interactions.domain.manager.ModalManager
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
    private val dialogManager: DialogManager,
    private val modalManager: ModalManager,
    logger: AppLogger,
) : MviViewModel<MainState, MainPartial, MainIntent, MainEffect>(
        initialState = settingsStore.read().let { settings ->
            MainState(
                darkTheme = settings.darkTheme,
                flow = if (sessionManager.user.value == null) AppFlow.Auth else AppFlow.Regular,
                dialog = dialogManager.currentDialog.value,
                modal = modalManager.currentModal.value,
            )
        },
        logger = logger,
    ) {
    override suspend fun processInit() {
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        modalManager.currentModal.bind { modal ->
            sendPartial(MainPartial.ModalUpdated(modal))
        }
        dialogManager.currentDialog.bind { dialog ->
            sendPartial(MainPartial.DialogUpdated(dialog))
        }
        sessionManager.user.bind { user ->
            val flow = if (user == null) AppFlow.Auth else AppFlow.Regular
            if (flow != currentState.flow) {
                dialogManager.dismissDialog()
                    modalManager.dismissModal()
                navigationManager.reset()
                sendPartial(MainPartial.FlowChanged(flow))
            }
        }
        navigationManager.activeDestination.bind { destination ->
            sendPartial(MainPartial.DestinationChanged(destination))
        }
        settingsStore.settings.bind { settings ->
            sendPartial(MainPartial.ThemeChanged(settings.darkTheme))
        }
    }

    override suspend fun handleIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.DismissDialog -> dialogManager.dismissDialog()
            is MainIntent.DismissModal -> {
                if (modalManager.currentModal.value?.id == intent.id) {
                    modalManager.dismissModal()
                }
            }
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
