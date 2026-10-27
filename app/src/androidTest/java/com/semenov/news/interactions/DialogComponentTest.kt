package com.semenov.news.interactions

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.semenov.news.core.ui.designsystem.theme.NewsTheme
import com.semenov.news.core.ui.interactions.domain.model.dialog.Action
import com.semenov.news.core.ui.interactions.domain.model.dialog.CheckboxAction
import com.semenov.news.core.ui.interactions.domain.model.dialog.DialogDestination
import com.semenov.news.core.ui.interactions.presentation.dialog.DialogComponent
import com.semenov.news.core.ui.interactions.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DialogComponentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noClosesLightDialogWithoutConfirmingLogout() {
        val actions = mutableListOf<String>()
        val dialog = mutableStateOf<DialogDestination?>(
            DialogDestination.Logout(
                primaryAction = Action(R.string.dialog_yes) { actions += "yes" },
                secondaryAction = Action(R.string.dialog_no) { actions += "no" },
            ),
        )
        setDialogContent(dialog, onDismiss = { actions += "dismiss" })
        captureDialog("dialog-light.png")

        composeRule.onNodeWithTag("dialog_secondary_action").performClick()

        composeRule.onNodeWithTag("app_dialog").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(listOf("no", "dismiss"), actions) }
    }

    @Test
    fun yesConfirmsExactlyOnceBeforeClosingDarkDialog() {
        val actions = mutableListOf<String>()
        val dialog = mutableStateOf<DialogDestination?>(
            DialogDestination.Logout(
                primaryAction = Action(R.string.dialog_yes) { actions += "yes" },
                secondaryAction = Action(R.string.dialog_no) { actions += "no" },
            ),
        )
        setDialogContent(dialog, darkTheme = true, onDismiss = { actions += "dismiss" })
        captureDialog("dialog-dark.png")

        composeRule.onNodeWithTag("dialog_primary_action").performClick()

        composeRule.onNodeWithTag("app_dialog").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(listOf("yes", "dismiss"), actions) }
    }

    @Test
    fun backDismissesCancelableDialogAndKeepsNonCancelableDialogOpen() {
        var dismissCount = 0
        val dialog = mutableStateOf<DialogDestination?>(DialogDestination.Logout())
        setDialogContent(dialog, onDismiss = { dismissCount++ })
        composeRule.onNodeWithTag("app_dialog").assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithTag("app_dialog").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, dismissCount)
            dialog.value = DialogDestination.Logout(
                cancelable = false,
                secondaryAction = Action(R.string.dialog_no),
            )
        }
        composeRule.onNodeWithTag("app_dialog").assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithTag("app_dialog").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, dismissCount) }
        composeRule.onNodeWithTag("dialog_secondary_action").performClick()
        composeRule.onNodeWithTag("app_dialog").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(2, dismissCount) }
    }

    @Test
    fun optionalCheckboxUpdatesItsStateAndReportsEachChangeWithoutDismissing() {
        val changes = mutableListOf<Boolean>()
        var dismissCount = 0
        val dialog = mutableStateOf<DialogDestination?>(
            DialogDestination.Logout(
                checkboxAction = CheckboxAction(
                    label = android.R.string.selectAll,
                    onClick = changes::add,
                ),
                secondaryAction = Action(R.string.dialog_no),
            ),
        )
        setDialogContent(dialog, onDismiss = { dismissCount++ })

        composeRule.onNodeWithTag("dialog_checkbox").assertIsOff().performClick().assertIsOn()
        composeRule.onNodeWithTag("dialog_checkbox").performClick().assertIsOff()

        composeRule.onNodeWithTag("app_dialog").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(listOf(true, false), changes)
            assertEquals(0, dismissCount)
        }
        composeRule.onNodeWithTag("dialog_secondary_action").performClick()
        composeRule.onNodeWithTag("app_dialog").assertDoesNotExist()
    }

    private fun setDialogContent(
        dialog: MutableState<DialogDestination?>,
        darkTheme: Boolean = false,
        onDismiss: () -> Unit,
    ) {
        composeRule.setContent {
            NewsTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().background(NewsTheme.colors.background)) {
                    DialogComponent(
                        dialog = dialog.value,
                        onDismissRequest = {
                            onDismiss()
                            dialog.value = null
                        },
                    )
                }
            }
        }
    }

    private fun captureDialog(filename: String) {
        composeRule.onNodeWithTag("app_dialog").assertIsDisplayed()
        val bitmap = composeRule.onNodeWithTag("app_dialog").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }
}
