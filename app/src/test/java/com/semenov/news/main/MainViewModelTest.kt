package com.semenov.news.main

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.navigation.domain.AppDestination
import com.semenov.news.core.ui.navigation.domain.NavigationManager
import com.semenov.news.core.ui.navigation.domain.AppFlow
import com.semenov.news.features.auth.domain.model.AuthUser
import com.semenov.news.features.auth.domain.session.SessionManager
import com.semenov.news.main.models.MainIntent
import com.semenov.news.main.reducer.MainReducer
import com.semenov.news.core.domain.settings.AppSettings
import com.semenov.news.core.domain.settings.AppSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `home is selected initially`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeNavigationManager())

        val state = viewModel.state
        runCurrent()

        assertEquals(AppDestination.Home, state.value.activeDestination)
    }

    @Test
    fun `destination intent switches the active navigation flow`() = runTest(dispatcher) {
        val navigationManager = FakeNavigationManager()
        val viewModel = createViewModel(navigationManager)
        val state = viewModel.state
        runCurrent()

        viewModel.processIntent(MainIntent.DestinationSelected(AppDestination.Categories))
        runCurrent()

        assertEquals(AppDestination.Categories, navigationManager.activeDestination.value)
        assertEquals(AppDestination.Categories, state.value.activeDestination)
    }

    @Test
    fun `navigation changes are reduced into main state`() = runTest(dispatcher) {
        val navigationManager = FakeNavigationManager()
        val viewModel = createViewModel(navigationManager)
        val state = viewModel.state
        runCurrent()

        navigationManager.navigate(AppDestination.Categories)
        runCurrent()

        assertEquals(AppDestination.Categories, state.value.activeDestination)
    }

    @Test
    fun `saved settings are available before initialization runs`() = runTest(dispatcher) {
        val settingsStore = FakeAppSettingsStore(AppSettings(darkTheme = true, hapticsEnabled = false))
        val viewModel = createViewModel(FakeNavigationManager(), settingsStore)

        assertEquals(true, viewModel.state.value.darkTheme)
    }

    @Test
    fun `default theme follows system until user makes a selection`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeNavigationManager())

        assertEquals(null, viewModel.state.value.darkTheme)
    }

    @Test
    fun `settings store changes update app theme and preserve navigation`() = runTest(dispatcher) {
        val navigationManager = FakeNavigationManager()
        val settingsStore = FakeAppSettingsStore()
        val viewModel = createViewModel(navigationManager, settingsStore)
        val state = viewModel.state
        runCurrent()
        viewModel.processIntent(MainIntent.DestinationSelected(AppDestination.Settings))
        runCurrent()

        settingsStore.setDarkTheme(true)
        settingsStore.setHapticsEnabled(false)
        runCurrent()

        assertEquals(AppSettings(darkTheme = true, hapticsEnabled = false), settingsStore.read())
        assertEquals(true, state.value.darkTheme)
        assertEquals(AppDestination.Settings, state.value.activeDestination)
    }

    @Test
    fun `explicit light theme and disabled haptics survive view model recreation`() = runTest(dispatcher) {
        val settingsStore = FakeAppSettingsStore()
        val firstViewModel = createViewModel(FakeNavigationManager(), settingsStore)
        firstViewModel.initialize()
        settingsStore.setDarkTheme(false)
        settingsStore.setHapticsEnabled(false)
        runCurrent()

        val recreatedViewModel = createViewModel(FakeNavigationManager(), settingsStore)

        assertEquals(false, recreatedViewModel.state.value.darkTheme)
    }

    @Test
    fun `missing user starts auth and ignores bottom navigation intents`() = runTest(dispatcher) {
        val navigationManager = FakeNavigationManager()
        val viewModel = createViewModel(navigationManager, sessionManager = FakeSessionManager(null))
        val state = viewModel.state
        runCurrent()

        viewModel.processIntent(MainIntent.DestinationSelected(AppDestination.Settings))
        runCurrent()

        assertEquals(AppFlow.Auth, state.value.flow)
        assertEquals(AppDestination.Home, navigationManager.activeDestination.value)
    }

    @Test
    fun `saved user starts regular flow before initialization`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeNavigationManager())

        assertEquals(AppFlow.Regular, viewModel.state.value.flow)
    }

    @Test
    fun `logout resets navigation and next login starts home`() = runTest(dispatcher) {
        val navigationManager = FakeNavigationManager()
        val session = FakeSessionManager(AuthUser("Viktor"))
        val viewModel = createViewModel(navigationManager, sessionManager = session)
        val state = viewModel.state
        runCurrent()
        viewModel.processIntent(MainIntent.DestinationSelected(AppDestination.Settings))
        runCurrent()

        session.logout()
        runCurrent()
        assertNull(session.user.value)
        assertEquals(AppFlow.Auth, state.value.flow)
        assertEquals(AppDestination.Home, state.value.activeDestination)

        session.saveUser(AuthUser("Anna"))
        runCurrent()
        assertEquals(AppFlow.Regular, state.value.flow)
        assertEquals(AppDestination.Home, state.value.activeDestination)
    }

    private fun createViewModel(
        navigationManager: NavigationManager,
        settingsStore: AppSettingsStore = FakeAppSettingsStore(),
        sessionManager: SessionManager = FakeSessionManager(AuthUser("Viktor")),
    ) =
        MainViewModel(
            navigationManager = navigationManager,
            reducer = MainReducer(),
            settingsStore = settingsStore,
            sessionManager = sessionManager,
            logger = NoOpLogger,
        )

    private class FakeAppSettingsStore(
        initial: AppSettings = AppSettings(),
    ) : AppSettingsStore {
        private val current = MutableStateFlow(initial)
        override val settings: StateFlow<AppSettings> = current

        override fun read(): AppSettings = settings.value

        override suspend fun setDarkTheme(enabled: Boolean) {
            current.value = current.value.copy(darkTheme = enabled)
        }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            current.value = current.value.copy(hapticsEnabled = enabled)
        }

        override suspend fun clear() {
            current.value = AppSettings()
        }
    }

    private class FakeSessionManager(initial: AuthUser?) : SessionManager {
        override val user = MutableStateFlow(initial)
        override suspend fun saveUser(user: AuthUser) { this.user.value = user }
        override suspend fun logout() { user.value = null }
    }

    private class FakeNavigationManager : NavigationManager {
        private val destination = MutableStateFlow<AppDestination>(AppDestination.Home)
        override val activeDestination: StateFlow<AppDestination> = destination

        override fun navigate(destination: AppDestination) {
            this.destination.value = destination
        }

        override fun goBack() {
            destination.value = AppDestination.Home
        }

        override fun reset() {
            destination.value = AppDestination.Home
        }
    }

    private object NoOpLogger : AppLogger {
        override fun log(
            tag: String,
            message: String,
            t: Throwable?,
        ) = Unit
    }
}
