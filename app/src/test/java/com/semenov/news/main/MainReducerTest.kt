package com.semenov.news.main

import com.semenov.news.core.ui.navigation.domain.AppDestination
import com.semenov.news.main.models.MainPartial
import com.semenov.news.main.models.MainState
import com.semenov.news.main.reducer.MainReducer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MainReducerTest {
    private val reducer = MainReducer()

    @Test
    fun `observed theme changes preserve destination`() = runTest {
        val state = MainState(activeDestination = AppDestination.Settings)

        val result = reducer.reduce(MainPartial.ThemeChanged(true), state)

        assertEquals(state.copy(darkTheme = true), result)
    }

    @Test
    fun `navigation changes preserve theme preference`() = runTest {
        val state = MainState(darkTheme = true)

        val result = reducer.reduce(MainPartial.DestinationChanged(AppDestination.Categories), state)

        assertEquals(state.copy(activeDestination = AppDestination.Categories), result)
    }
}
