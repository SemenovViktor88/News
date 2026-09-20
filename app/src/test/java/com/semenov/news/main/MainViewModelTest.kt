package com.semenov.news.main

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.navigation.domain.AppDestination
import com.semenov.news.core.ui.navigation.domain.NavigationManager
import com.semenov.news.main.models.MainIntent
import com.semenov.news.main.reducer.MainReducer
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

    private fun createViewModel(navigationManager: NavigationManager) =
        MainViewModel(
            navigationManager = navigationManager,
            reducer = MainReducer(),
            logger = NoOpLogger,
        )

    private class FakeNavigationManager : NavigationManager {
        private val destination = MutableStateFlow<AppDestination>(AppDestination.Home)
        override val activeDestination: StateFlow<AppDestination> = destination

        override fun navigate(destination: AppDestination) {
            this.destination.value = destination
        }

        override fun goBack() {
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
