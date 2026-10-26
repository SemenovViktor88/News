package com.semenov.news.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.semenov.news.screens.settings.SettingsContent
import com.semenov.news.core.ui.designsystem.theme.NewsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun themeRowChangesThemeWithoutChangingHaptics() {
        setSettingsContent()

        composeRule.onNodeWithTag("settings_dark_theme").assertIsOff().performClick()
        settleTransition()

        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_dark_theme").assertIsOn()
        composeRule.onNodeWithTag("settings_haptics").assertIsOn()

        composeRule.onNodeWithTag("settings_dark_theme").performClick()
        settleTransition()

        composeRule.onNodeWithTag("settings_dark_theme").assertIsOff()
        composeRule.onNodeWithTag("settings_haptics").assertIsOn()
    }

    @Test
    fun lampAccessibilityActionChangesThemeWithHapticsDisabled() {
        setSettingsContent(initialHapticsEnabled = false)

        composeRule.onNodeWithContentDescription("Pull the cord to change theme")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        settleTransition()

        composeRule.onNodeWithTag("settings_dark_theme").assertIsOn()
        composeRule.onNodeWithTag("settings_haptics").assertIsOff()

        composeRule.onNodeWithContentDescription("Pull the cord to change theme")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        settleTransition()

        composeRule.onNodeWithTag("settings_dark_theme").assertIsOff()
        composeRule.onNodeWithTag("settings_haptics").assertIsOff()
    }

    @Test
    fun hapticsCanBeChangedIndependentlyInDarkTheme() {
        setSettingsContent(initialDarkTheme = true)

        composeRule.onNodeWithTag("settings_haptics").assertIsOn().performClick()
        settleTransition()

        composeRule.onNodeWithTag("settings_haptics").assertIsOff()
        composeRule.onNodeWithTag("settings_dark_theme").assertIsOn()

        composeRule.onNodeWithTag("settings_haptics").performClick()
        settleTransition()

        composeRule.onNodeWithTag("settings_haptics").assertIsOn()
        composeRule.onNodeWithTag("settings_dark_theme").assertIsOn()
    }

    private fun setSettingsContent(
        initialDarkTheme: Boolean = false,
        initialHapticsEnabled: Boolean = true,
    ) {
        composeRule.setContent {
            var darkTheme by remember { mutableStateOf(initialDarkTheme) }
            var hapticsEnabled by remember { mutableStateOf(initialHapticsEnabled) }

            NewsTheme(darkTheme = darkTheme) {
                SettingsContent(
                    darkTheme = darkTheme,
                    hapticsEnabled = hapticsEnabled,
                    onDarkThemeChange = { darkTheme = it },
                    onHapticsChange = { hapticsEnabled = it },
                )
            }
        }
    }

    private fun settleTransition() {
        composeRule.mainClock.advanceTimeBy(1_500)
        composeRule.waitForIdle()
    }
}
