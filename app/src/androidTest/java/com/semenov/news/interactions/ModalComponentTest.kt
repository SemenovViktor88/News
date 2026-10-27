package com.semenov.news.interactions

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.AnnotatedString
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.semenov.news.core.ui.designsystem.theme.NewsTheme
import com.semenov.news.core.ui.interactions.domain.model.modal.ModalDestination
import com.semenov.news.core.ui.interactions.domain.model.modal.VerificationCodeState
import com.semenov.news.core.ui.interactions.presentation.modal.ModalComponent
import com.semenov.news.core.ui.interactions.presentation.modal.VerificationAnimationPhase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModalComponentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fourCodeCellsSupportPasteAndBackspaceInBothThemes() {
        val codeState = MutableStateFlow(VerificationCodeState())
        val modal = mutableStateOf<ModalDestination?>(verificationModal(codeState))
        val darkTheme = mutableStateOf(false)
        setModalContent(modal, darkTheme)

        repeat(4) { index ->
            composeRule.onNodeWithTag("auth_code_cell_$index", useUnmergedTree = true).assertIsDisplayed()
        }
        captureModal("modal-light.png")

        composeRule.onNodeWithTag("auth_code").performTextReplacement("1a2-345")
        assertEditableCode("1234")
        composeRule.onNodeWithTag("auth_code").performKeyInput { pressKey(Key.Backspace) }
        assertEditableCode("123")

        composeRule.runOnIdle {
            assertEquals("123", codeState.value.code)
            darkTheme.value = true
        }
        captureModal("modal-dark.png")
    }

    @Test
    fun backDismissesOnceAndProgrammaticDismissDoesNotCallOnDismiss() {
        val codeState = MutableStateFlow(VerificationCodeState())
        var callbackCount = 0
        var requestCount = 0
        val modal = mutableStateOf<ModalDestination?>(
            verificationModal(codeState, onDismiss = { callbackCount++ }),
        )
        setModalContent(modal, onDismissRequest = { requestCount++ })
        composeRule.onNodeWithTag("auth_modal").assertIsDisplayed()

        dismissUsingBack()

        composeRule.onNodeWithTag("auth_modal").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, callbackCount)
            assertEquals(1, requestCount)
            modal.value = verificationModal(codeState, onDismiss = { callbackCount++ })
        }
        composeRule.onNodeWithTag("auth_modal").assertIsDisplayed()

        composeRule.runOnIdle { modal.value = null }

        composeRule.onNodeWithTag("auth_modal").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, callbackCount)
            assertEquals(1, requestCount)
        }
    }

    @Test
    fun submittingSuccessfulAndNonDismissibleModalsIgnoreBack() {
        val codeState = MutableStateFlow(VerificationCodeState(isSubmitting = true))
        var callbackCount = 0
        var requestCount = 0
        val modal = mutableStateOf<ModalDestination?>(
            verificationModal(codeState, onDismiss = { callbackCount++ }),
        )
        setModalContent(modal, onDismissRequest = { requestCount++ })

        dismissUsingBack()
        composeRule.onNodeWithTag("auth_modal").assertIsDisplayed()

        composeRule.runOnIdle { codeState.value = VerificationCodeState(isVerified = true) }
        dismissUsingBack()
        composeRule.onNodeWithTag("auth_modal").assertIsDisplayed()

        composeRule.runOnIdle {
            codeState.value = VerificationCodeState()
            modal.value = verificationModal(
                codeState,
                isDismissible = false,
                onDismiss = { callbackCount++ },
            )
        }
        dismissUsingBack()
        composeRule.onNodeWithTag("auth_modal").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(0, callbackCount)
            assertEquals(0, requestCount)
        }
    }

    @Test
    fun successfulCodeFinishesMorphBeforeNotifyingAndNotifiesOnlyOnce() {
        val codeState = MutableStateFlow(VerificationCodeState(code = "1111"))
        val darkTheme = mutableStateOf(false)
        var completionCount = 0
        val modal = mutableStateOf<ModalDestination?>(
            verificationModal(codeState, onSuccessAnimationFinished = { completionCount++ }),
        )
        setModalContent(modal, darkTheme)
        closeSoftKeyboard()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnIdle { codeState.value = codeState.value.copy(isVerified = true) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(200)
        assertAnimationStage("tracing", completionCount = { completionCount })
        captureModal("otp-tracing.png")

        composeRule.mainClock.advanceTimeBy(400)
        assertAnimationStage("merging", completionCount = { completionCount })
        captureModal("otp-merging.png")

        composeRule.mainClock.advanceTimeBy(400)
        assertAnimationStage("check", completionCount = { completionCount })
        captureModal("otp-check.png")

        composeRule.mainClock.advanceTimeBy(300)
        assertAnimationStage("success", completionCount = { completionCount })
        captureModal("otp-success.png")

        composeRule.mainClock.advanceTimeBy(400)
        composeRule.runOnIdle {
            assertEquals(1, completionCount)
            // A theme change recomposes the host without starting the completed sequence again.
            darkTheme.value = true
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.runOnIdle { assertEquals(1, completionCount) }
    }

    @Test
    fun removingModalDuringSuccessfulCodeAnimationCancelsCompletion() {
        val codeState = MutableStateFlow(VerificationCodeState(code = "1111"))
        var completionCount = 0
        val modal = mutableStateOf<ModalDestination?>(
            verificationModal(codeState, onSuccessAnimationFinished = { completionCount++ }),
        )
        setModalContent(modal)
        closeSoftKeyboard()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnIdle { codeState.value = codeState.value.copy(isVerified = true) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(600)
        assertAnimationStage("merging", completionCount = { completionCount })

        composeRule.runOnIdle { modal.value = null }
        composeRule.mainClock.advanceTimeBy(2_000)

        composeRule.onNodeWithTag("auth_modal").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, completionCount) }
    }

    private fun verificationModal(
        state: MutableStateFlow<VerificationCodeState>,
        isDismissible: Boolean = true,
        onDismiss: (() -> Unit)? = null,
        onSuccessAnimationFinished: () -> Unit = {},
    ) = ModalDestination.VerificationCode(
        userName = "Reader",
        state = state,
        onCodeChanged = { state.value = state.value.copy(code = it) },
        onVerify = {},
        onResend = {},
        isDismissible = isDismissible,
        onDismiss = onDismiss,
        onSuccessAnimationFinished = onSuccessAnimationFinished,
    )

    private fun setModalContent(
        modal: MutableState<ModalDestination?>,
        darkTheme: MutableState<Boolean> = mutableStateOf(false),
        onDismissRequest: () -> Unit = {},
    ) {
        composeRule.setContent {
            NewsTheme(darkTheme = darkTheme.value) {
                Box(Modifier.fillMaxSize().background(NewsTheme.colors.background)) {
                    ModalComponent(
                        modal = modal.value,
                        requestDismiss = {
                            onDismissRequest()
                            modal.value = null
                        },
                    )
                }
            }
        }
    }

    private fun dismissUsingBack() {
        composeRule.waitForIdle()
        closeSoftKeyboard()
        composeRule.waitForIdle()
        pressBack()
        composeRule.waitForIdle()
    }

    private fun assertEditableCode(value: String) {
        composeRule.onNodeWithTag("auth_code").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(value)),
        )
    }

    private fun assertAnimationStage(stage: String, completionCount: () -> Int) {
        composeRule.onNodeWithTag("auth_verification_animation").assert(
            SemanticsMatcher.expectValue(VerificationAnimationPhase, stage),
        )
        composeRule.runOnIdle { assertEquals(0, completionCount()) }
    }

    private fun captureModal(filename: String) {
        composeRule.onNodeWithTag("auth_modal").assertIsDisplayed()
        closeSoftKeyboard()
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Material's sheet owns a separate native window; capture the composed display.
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val context = instrumentation.targetContext
        context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }
}
