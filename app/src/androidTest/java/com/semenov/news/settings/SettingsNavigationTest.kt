package com.semenov.news.settings

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.semenov.news.main.MainActivity
import com.semenov.news.core.ui.designsystem.theme.newsPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun signInIfNeeded() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("auth_username"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("navigation_settings"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("auth_username"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("auth_username").performTextInput("Settings test")
            composeRule.onNodeWithTag("auth_password").performTextInput("news")
            composeRule.onNodeWithTag("auth_sign_in").performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("auth_code"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("auth_code").performTextInput("1111")
            composeRule.onNodeWithTag("auth_verify").performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("navigation_settings"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun settingsRemainSelectedAcrossNavigationAndActivityRecreation() {
        openSettings()
        val initialDarkTheme = isChecked("settings_dark_theme")
        val initialHaptics = isChecked("settings_haptics")

        setChecked("settings_dark_theme", false)
        assertChecked("settings_haptics", initialHaptics)
        captureThemeScreens(darkTheme = false)

        setChecked("settings_dark_theme", true)
        assertChecked("settings_haptics", initialHaptics)
        captureThemeScreens(darkTheme = true)

        setChecked("settings_haptics", !initialHaptics)
        assertChecked("settings_dark_theme", true)

        composeRule.onNodeWithTag("navigation_home").performClick()
        settleTransition()
        openSettings()
        assertChecked("settings_dark_theme", true)
        assertChecked("settings_haptics", !initialHaptics)

        composeRule.activityRule.scenario.recreate()
        settleTransition()
        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        assertChecked("settings_dark_theme", true)
        assertChecked("settings_haptics", !initialHaptics)

        setChecked("settings_dark_theme", initialDarkTheme)
        setChecked("settings_haptics", initialHaptics)
    }

    private fun openSettings() {
        composeRule.onNodeWithTag("navigation_settings").performClick()
        settleTransition()
        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_dark_theme").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_haptics").assertIsDisplayed()
    }

    private fun isChecked(tag: String): Boolean =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.ToggleableState] ==
            ToggleableState.On

    private fun setChecked(tag: String, checked: Boolean) {
        if (isChecked(tag) != checked) {
            composeRule.onNodeWithTag(tag).performClick()
        }
        settleTransition()
        assertChecked(tag, checked)
    }

    private fun assertChecked(tag: String, checked: Boolean) {
        val node = composeRule.onNodeWithTag(tag)
        if (checked) node.assertIsOn() else node.assertIsOff()
    }

    private fun settleTransition() {
        composeRule.mainClock.advanceTimeBy(4_000)
        composeRule.waitForIdle()
    }

    private fun captureThemeScreens(darkTheme: Boolean) {
        val suffix = if (darkTheme) "dark" else "light"
        captureScreen("settings-$suffix.png", darkTheme)
        composeRule.onNodeWithTag("navigation_home").performClick()
        settleTransition()
        captureScreen("home-$suffix.png", darkTheme)
        composeRule.onNodeWithTag("navigation_categories").performClick()
        settleTransition()
        captureScreen("categories-$suffix.png", darkTheme)
        openSettings()
    }

    private fun captureScreen(filename: String, darkTheme: Boolean) {
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        assertEquals(
            "All tabs must use the shared theme background: $filename",
            newsPalette(darkTheme).background.toArgb(),
            // Sample above the lamp: its diffuse glow also lights the page's side edges.
            bitmap.getPixel(4, 4),
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val saved = context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        assertTrue("Could not save $filename for visual verification", saved)
    }
}
