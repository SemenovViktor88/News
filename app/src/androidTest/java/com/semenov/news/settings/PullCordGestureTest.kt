package com.semenov.news.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.semenov.news.screens.settings.SettingsContent
import com.semenov.news.core.ui.designsystem.theme.NewsTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PullCordGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sidewaysAndShortDownwardDragsDoNotToggleTheme() {
        val changes = setSettingsContent()

        composeRule.onNodeWithTag(LAMP_GESTURES).performTouchInput {
            val handle = Offset(width * .7f + 18.dp.toPx(), 220.dp.toPx())
            swipe(start = handle, end = handle - Offset(60.dp.toPx(), 0f), durationMillis = 350)
        }
        settleCord()

        assertEquals("A sideways swing must not change the theme", 0, changes.get())
        composeRule.onNodeWithTag("settings_dark_theme").assertIsOff()

        composeRule.onNodeWithTag(LAMP_GESTURES).performTouchInput {
            val handle = Offset(width * .7f + 18.dp.toPx(), 220.dp.toPx())
            swipe(start = handle, end = handle + Offset(0f, 25.dp.toPx()), durationMillis = 350)
        }
        settleCord()

        assertEquals("A pull below the 48 dp threshold must not change the theme", 0, changes.get())
        composeRule.onNodeWithTag("settings_dark_theme").assertIsOff()
        composeRule.onNodeWithTag("settings_haptics").assertIsOff()
    }

    @Test
    fun downwardPullTogglesOncePerGestureAndWorksAgainAfterRelease() {
        val changes = setSettingsContent()

        pullPastThreshold()
        settleCord()

        assertEquals("Continuing past the trigger must toggle only once", 1, changes.get())
        composeRule.onNodeWithTag("settings_dark_theme").assertIsOn()
        composeRule.onNodeWithTag("settings_haptics").assertIsOff()

        pullPastThreshold()
        settleCord()

        assertEquals("Releasing the cord must arm the next pull", 2, changes.get())
        composeRule.onNodeWithTag("settings_dark_theme").assertIsOff()
        composeRule.onNodeWithTag("settings_haptics").assertIsOff()
    }

    private fun pullPastThreshold() {
        composeRule.onNodeWithTag(LAMP_GESTURES).performTouchInput {
            val handle = Offset(width * .7f + 18.dp.toPx(), 220.dp.toPx())
            down(handle)
            moveTo(handle + Offset(0f, 10.dp.toPx()))
            moveTo(handle + Offset(0f, 55.dp.toPx()))
            moveTo(handle + Offset(0f, 64.dp.toPx()))
            // Crossing the threshold again while the same finger is down must not retrigger.
            moveTo(handle + Offset(0f, 35.dp.toPx()))
            moveTo(handle + Offset(0f, 64.dp.toPx()))
            up()
        }
    }

    private fun setSettingsContent(): AtomicInteger {
        val changes = AtomicInteger()
        composeRule.setContent {
            var darkTheme by remember { mutableStateOf(false) }
            var hapticsEnabled by remember { mutableStateOf(false) }

            NewsTheme(darkTheme = darkTheme) {
                // Exercise the actual screen, including its vertical scrolling parent.
                SettingsContent(
                    darkTheme = darkTheme,
                    hapticsEnabled = hapticsEnabled,
                    onDarkThemeChange = {
                        changes.incrementAndGet()
                        darkTheme = it
                    },
                    onHapticsChange = { hapticsEnabled = it },
                )
            }
        }
        return changes
    }

    private fun settleCord() {
        composeRule.mainClock.advanceTimeBy(4_000)
        composeRule.waitForIdle()
    }

    private companion object {
        const val LAMP_GESTURES = "settings_lamp_gestures"
    }
}
