package com.semenov.news.interactions

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.semenov.news.core.domain.settings.AppSettings
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.ui.interactions.domain.manager.DialogManager
import com.semenov.news.core.ui.interactions.domain.model.dialog.DialogDestination
import com.semenov.news.features.auth.domain.model.AuthUser
import com.semenov.news.features.auth.domain.session.SessionManager
import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.interactions.data.manager.HapticEffectManagerImpl
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import com.semenov.news.core.ui.interactions.presentation.haptic.HapticEffectHandler
import com.semenov.news.screens.settings.SettingsScreen
import com.semenov.news.screens.settings.SettingsViewModel
import com.semenov.news.screens.settings.reducer.SettingsReducer
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HapticEffectHandlerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun managerEffectsReachPlatformFeedbackExactlyOnce() {
        val manager = HapticEffectManagerImpl(FakeSettingsStore())
        val feedback = RecordingHapticFeedback()
        val owner = TestLifecycleOwner()
        setHandler(manager, feedback, owner)
        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            manager.performBlocking(AppHapticEffect.Click)
            manager.performBlocking(AppHapticEffect.LongPress)
            manager.performBlocking(AppHapticEffect.Confirm)
            manager.performBlocking(AppHapticEffect.Reject)
            manager.performBlocking(AppHapticEffect.ToggleOn)
            manager.performBlocking(AppHapticEffect.ToggleOff)
            manager.performBlocking(AppHapticEffect.SegmentTick)
            manager.performBlocking(AppHapticEffect.SegmentFrequentTick)
            manager.performBlocking(AppHapticEffect.GestureThresholdActivate)
        }
        runBlocking { manager.perform(AppHapticEffect.GestureEnd) }

        composeRule.waitUntil { feedback.performed.size == 10 }
        assertEquals(
            listOf(
                HapticFeedbackType.VirtualKey,
                HapticFeedbackType.LongPress,
                HapticFeedbackType.Confirm,
                HapticFeedbackType.Reject,
                HapticFeedbackType.ToggleOn,
                HapticFeedbackType.ToggleOff,
                HapticFeedbackType.SegmentTick,
                HapticFeedbackType.SegmentFrequentTick,
                HapticFeedbackType.GestureThresholdActivate,
                HapticFeedbackType.GestureEnd,
            ),
            feedback.performed.toList(),
        )
    }

    @Test
    fun stoppedLifecycleDoesNotPlayOrReplayEffectsAfterRestart() {
        val manager = HapticEffectManagerImpl(FakeSettingsStore())
        val feedback = RecordingHapticFeedback()
        val owner = TestLifecycleOwner()
        setHandler(manager, feedback, owner)
        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        composeRule.waitForIdle()

        composeRule.runOnIdle { manager.performBlocking(AppHapticEffect.Click) }
        composeRule.waitUntil { feedback.performed.size == 1 }

        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        composeRule.waitForIdle()
        composeRule.runOnIdle { manager.performBlocking(AppHapticEffect.Confirm) }
        composeRule.waitForIdle()
        assertEquals(listOf(HapticFeedbackType.VirtualKey), feedback.performed.toList())

        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        composeRule.waitForIdle()
        assertEquals(listOf(HapticFeedbackType.VirtualKey), feedback.performed.toList())
        composeRule.runOnIdle { manager.performBlocking(AppHapticEffect.Reject) }
        composeRule.waitUntil { feedback.performed.size == 2 }
        assertEquals(
            listOf(HapticFeedbackType.VirtualKey, HapticFeedbackType.Reject),
            feedback.performed.toList(),
        )
    }

    @Test
    fun settingsTogglesConfirmChangesAndRespectDisabledFeedback() {
        val settings = FakeSettingsStore()
        val manager = HapticEffectManagerImpl(settings)
        val feedback = RecordingHapticFeedback()
        val owner = TestLifecycleOwner()
        setHandler(manager, feedback, owner, settings)
        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_dark_theme").assertIsOff().performClick().assertIsOn()
        composeRule.waitUntil { feedback.performed.size == 1 }
        composeRule.onNodeWithTag("settings_dark_theme").performClick().assertIsOff()
        composeRule.waitUntil { feedback.performed.size == 2 }
        assertEquals(
            listOf(HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff),
            feedback.performed.toList(),
        )

        composeRule.runOnIdle { manager.performBlocking(AppHapticEffect.Click) }
        composeRule.waitUntil { feedback.performed.size == 3 }

        composeRule.onNodeWithTag("settings_haptics").assertIsOn().performClick().assertIsOff()
        composeRule.waitUntil { feedback.performed.size == 4 }
        composeRule.onNodeWithTag("settings_dark_theme").performClick().assertIsOn()
        composeRule.runOnIdle { manager.performBlocking(AppHapticEffect.Confirm) }
        runBlocking { manager.perform(AppHapticEffect.LongPress) }
        composeRule.waitForIdle()
        assertEquals(
            listOf(
                HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff,
                HapticFeedbackType.VirtualKey, HapticFeedbackType.ToggleOff,
            ),
            feedback.performed.toList(),
        )

        composeRule.onNodeWithTag("settings_haptics").performClick().assertIsOn()
        composeRule.waitUntil { feedback.performed.size == 5 }
        runBlocking { manager.perform(AppHapticEffect.Reject) }
        composeRule.waitUntil { feedback.performed.size == 6 }
        assertEquals(
            listOf(
                HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff,
                HapticFeedbackType.VirtualKey, HapticFeedbackType.ToggleOff,
                HapticFeedbackType.ToggleOn, HapticFeedbackType.Reject,
            ),
            feedback.performed.toList(),
        )
    }

    private fun setHandler(
        manager: HapticEffectManagerImpl,
        feedback: RecordingHapticFeedback,
        owner: TestLifecycleOwner,
        settings: FakeSettingsStore? = null,
    ) {
        val viewModel = settings?.let {
            SettingsViewModel(
                settingsStore = it,
                settingsReducer = SettingsReducer(),
                hapticEffectManager = manager,
                sessionManager = object : SessionManager {
                    override val user = MutableStateFlow<AuthUser?>(AuthUser("Test"))
                    override suspend fun saveUser(user: AuthUser) { this.user.value = user }
                    override suspend fun logout() { user.value = null }
                },
                dialogManager = object : DialogManager {
                    override val currentDialog = MutableStateFlow<DialogDestination?>(null)
                    override suspend fun showDialog(dialog: DialogDestination) { currentDialog.value = dialog }
                    override suspend fun dismissDialog() { currentDialog.value = null }
                },
                logger = object : AppLogger {
                    override fun log(tag: String, message: String, t: Throwable?) = Unit
                },
            )
        }
        composeRule.setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides owner,
                LocalHapticFeedback provides feedback,
            ) {
                HapticEffectHandler(hapticEffects = manager.effects)
                if (viewModel != null) {
                    DisposableEffect(viewModel) {
                        onDispose { viewModel.viewModelScope.cancel() }
                    }
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    private class FakeSettingsStore : AppSettingsStore {
        private val values = MutableStateFlow(AppSettings(darkTheme = false))
        override val settings = values.asStateFlow()

        override fun read(): AppSettings = values.value

        override suspend fun setDarkTheme(enabled: Boolean) {
            values.update { it.copy(darkTheme = enabled) }
        }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            values.update { it.copy(hapticsEnabled = enabled) }
        }

        override suspend fun clear() {
            values.value = AppSettings()
        }
    }

    private class RecordingHapticFeedback : HapticFeedback {
        val performed = CopyOnWriteArrayList<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }
}
