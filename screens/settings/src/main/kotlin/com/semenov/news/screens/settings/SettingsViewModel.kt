package com.semenov.news.screens.settings

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.settings.AppSettings
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import com.semenov.news.core.ui.interactions.domain.manager.DialogManager
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import com.semenov.news.core.ui.interactions.domain.model.dialog.Action
import com.semenov.news.core.ui.interactions.domain.model.dialog.DialogDestination
import com.semenov.news.features.auth.domain.model.AuthUser
import com.semenov.news.features.auth.domain.session.SessionManager
import com.semenov.news.screens.settings.model.SettingsEffect
import com.semenov.news.screens.settings.model.SettingsIntent
import com.semenov.news.screens.settings.model.SettingsPartial
import com.semenov.news.screens.settings.model.SettingsState
import com.semenov.news.screens.settings.reducer.SettingsReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
    private val settingsReducer: SettingsReducer,
    private val hapticEffectManager: HapticEffectManager,
    private val sessionManager: SessionManager,
    private val dialogManager: DialogManager,
    logger: AppLogger,
) : MviViewModel<SettingsState, SettingsPartial, SettingsIntent, SettingsEffect>(
        initialState = settingsStore.read().toState(sessionManager.user.value?.name.orEmpty()),
        logger = logger,
    ) {
    private val intentMutex = Mutex()
    private var nextLogoutConfirmationId = 0L
    private var logoutConfirmation: LogoutConfirmation? = null

    override suspend fun processInit() {
        launch {
            sessionManager.user.collect { user ->
                if (logoutConfirmation?.user !== user) logoutConfirmation = null
                sendPartial(SettingsPartial.UserChanged(user?.name.orEmpty()))
            }
        }
        settingsStore.settings.collect { settings ->
            sendPartial(
                SettingsPartial.PreferencesChanged(
                    darkTheme = settings.darkTheme,
                    hapticsEnabled = settings.hapticsEnabled,
                ),
            )
        }
    }

    override suspend fun handleIntent(intent: SettingsIntent) = intentMutex.withLock {
        if (currentState.isLoggingOut || sessionManager.user.value == null) return@withLock
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
            SettingsIntent.LogoutClicked -> {
                showLogoutConfirmation()
            }
            is SettingsIntent.LogoutConfirmed -> {
                val confirmation = logoutConfirmation
                if (confirmation == null || confirmation.id != intent.confirmationId ||
                    confirmation.user !== sessionManager.user.value
                ) return@withLock
                logoutConfirmation = null
                sendPartial(SettingsPartial.LogoutStarted)
                try {
                    hapticEffectManager.perform(AppHapticEffect.Confirm)
                    sessionManager.logout()
                    sendPartial(SettingsPartial.LogoutFinished)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    logError(error, source = "logout")
                    sendPartial(SettingsPartial.LogoutFailed)
                }
            }
        }
    }

    private suspend fun showLogoutConfirmation() {
        val user = sessionManager.user.value ?: return
        val confirmationId = ++nextLogoutConfirmationId
        logoutConfirmation = LogoutConfirmation(confirmationId, user)
        lateinit var dialog: DialogDestination.Logout
        dialog = DialogDestination.Logout(
            primaryAction = Action(label = R.string.settings_logout_yes) {
                // Capture the decision before the host dismisses the dialog.
                if (dialogManager.currentDialog.value === dialog) {
                    processIntent(SettingsIntent.LogoutConfirmed(confirmationId))
                }
            },
            secondaryAction = Action(label = R.string.settings_logout_no),
        )
        hapticEffectManager.perform(AppHapticEffect.Click)
        dialogManager.showDialog(dialog)
    }

    override suspend fun reduceWithPartial(
        partial: SettingsPartial,
        old: SettingsState,
    ): SettingsState = settingsReducer.reduce(partial, old)

    private data class LogoutConfirmation(val id: Long, val user: AuthUser)
}

private fun AppSettings.toState(userName: String) =
    SettingsState(
        darkTheme = darkTheme,
        hapticsEnabled = hapticsEnabled,
        userName = userName,
    )
