package com.semenov.news.screens.settings.reducer

import com.semenov.news.screens.settings.model.SettingsPartial
import com.semenov.news.screens.settings.model.SettingsState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsReducerTest {
    private val reducer = SettingsReducer()

    @Test
    fun `preference snapshot replaces both choices together`() = runTest {
        val previous = SettingsState(darkTheme = false, hapticsEnabled = true)

        val next =
            reducer.reduce(
                SettingsPartial.PreferencesChanged(darkTheme = true, hapticsEnabled = false),
                previous,
            )

        assertEquals(true, next.darkTheme)
        assertFalse(next.hapticsEnabled)
        assertEquals(SettingsState(darkTheme = false, hapticsEnabled = true), previous)
        assertTrue(next.hasContent)
        assertNull(next.initialErrorState)
    }

    @Test
    fun `system theme snapshot clears an explicit theme choice`() = runTest {
        val next =
            reducer.reduce(
                SettingsPartial.PreferencesChanged(darkTheme = null, hapticsEnabled = true),
                SettingsState(darkTheme = true, hapticsEnabled = false),
            )

        assertNull(next.darkTheme)
        assertTrue(next.hapticsEnabled)
    }
}
