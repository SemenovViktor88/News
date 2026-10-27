package com.semenov.news.main

import com.semenov.news.core.ui.navigation.domain.AppDestination
import com.semenov.news.core.ui.interactions.domain.model.dialog.DialogDestination
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

    @Test
    fun `dialog changes preserve current destination and theme`() = runTest {
        val state = MainState(activeDestination = AppDestination.Settings, darkTheme = true)
        val dialog = DialogDestination.Logout()

        assertEquals(state.copy(dialog = dialog), reducer.reduce(MainPartial.DialogUpdated(dialog), state))
        assertEquals(state, reducer.reduce(MainPartial.DialogUpdated(null), state.copy(dialog = dialog)))
    }
}
