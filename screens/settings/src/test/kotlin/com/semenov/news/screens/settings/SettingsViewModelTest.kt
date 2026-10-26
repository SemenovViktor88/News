package com.semenov.news.screens.settings

import androidx.lifecycle.viewModelScope
import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.settings.AppSettings
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import com.semenov.news.features.auth.domain.model.AuthUser
import com.semenov.news.features.auth.domain.session.SessionManager
import com.semenov.news.screens.settings.model.SettingsIntent
import com.semenov.news.screens.settings.reducer.SettingsReducer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val viewModels = mutableListOf<SettingsViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun `saved preferences are available before initialization runs`() = runTest(dispatcher) {
        val store = FakeSettingsStore(AppSettings(darkTheme = true, hapticsEnabled = false))
        val viewModel = createViewModel(store)

        assertEquals(true, viewModel.state.value.darkTheme)
        assertFalse(viewModel.state.value.hapticsEnabled)
        assertTrue(viewModel.state.value.hasContent)
        assertNull(viewModel.state.value.initialErrorState)
        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun `default theme continues to follow system until an explicit choice`() = runTest(dispatcher) {
        val store = FakeSettingsStore()
        val viewModel = createViewModel(store)
        advanceUntilIdle()

        assertNull(viewModel.state.value.darkTheme)
        assertTrue(store.writes.isEmpty())

        viewModel.processIntent(SettingsIntent.DarkThemeChanged(true))
        advanceUntilIdle()

        assertEquals(AppSettings(darkTheme = true), store.read())
        assertEquals(true, viewModel.state.value.darkTheme)
        assertEquals(listOf("darkTheme:true"), store.writes)
    }

    @Test
    fun `haptic feedback preference persists without replacing system theme selection`() = runTest(dispatcher) {
        val store = FakeSettingsStore()
        val viewModel = createViewModel(store)
        val state = viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(SettingsIntent.HapticsChanged(false))
        advanceUntilIdle()

        assertEquals(AppSettings(darkTheme = null, hapticsEnabled = false), store.read())
        assertNull(state.value.darkTheme)
        assertFalse(state.value.hapticsEnabled)
        assertEquals(listOf("haptics:false"), store.writes)
    }

    @Test
    fun `external preference changes are reflected while settings is open`() = runTest(dispatcher) {
        val store = FakeSettingsStore(AppSettings(darkTheme = false))
        val viewModel = createViewModel(store)
        val state = viewModel.state
        advanceUntilIdle()

        store.replace(AppSettings(darkTheme = true, hapticsEnabled = false))
        advanceUntilIdle()

        assertEquals(true, state.value.darkTheme)
        assertFalse(state.value.hapticsEnabled)
        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun `consecutive preference changes keep both values and restore in a new screen`() =
        runTest(dispatcher) {
            val store = FakeSettingsStore()
            val viewModel = createViewModel(store)
            val state = viewModel.state
            advanceUntilIdle()

            viewModel.processIntent(SettingsIntent.DarkThemeChanged(true))
            viewModel.processIntent(SettingsIntent.HapticsChanged(false))
            advanceUntilIdle()

            assertEquals(true, state.value.darkTheme)
            assertFalse(state.value.hapticsEnabled)
            val restored = createViewModel(store).state.value
            assertEquals(state.value, restored)
        }

    @Test
    fun `repeated initialization observes preferences once`() = runTest(dispatcher) {
        val store = FakeSettingsStore()
        val viewModel = createViewModel(store)
        viewModel.state
        repeat(3) { viewModel.initialize() }
        advanceUntilIdle()

        assertEquals(1, store.subscriptionCount)
    }

    @Test
    fun `lamp pull requests feedback and changes theme`() = runTest(dispatcher) {
        val store = FakeSettingsStore(AppSettings(darkTheme = false, hapticsEnabled = true))
        val haptics = FakeHapticEffectManager()
        val viewModel = createViewModel(store, haptics)
        viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(SettingsIntent.LampPulled(darkTheme = true))
        advanceUntilIdle()

        assertEquals(listOf(AppHapticEffect.LongPress), haptics.performed)
        assertEquals(AppSettings(darkTheme = true, hapticsEnabled = true), store.read())
        assertEquals(true, viewModel.state.value.darkTheme)
    }

    @Test
    fun `theme toggle requests directional haptics only for a changed value`() = runTest(dispatcher) {
        val store = FakeSettingsStore(AppSettings(darkTheme = false))
        val haptics = FakeHapticEffectManager()
        val viewModel = createViewModel(store, haptics)

        viewModel.processIntent(SettingsIntent.DarkThemeChanged(true))
        advanceUntilIdle()
        viewModel.processIntent(SettingsIntent.DarkThemeChanged(false))
        advanceUntilIdle()
        viewModel.processIntent(SettingsIntent.DarkThemeChanged(false))
        advanceUntilIdle()

        assertEquals(listOf(AppHapticEffect.ToggleOn, AppHapticEffect.ToggleOff), haptics.performed)
        assertEquals(listOf("darkTheme:true", "darkTheme:false"), store.writes)
    }

    @Test
    fun `haptics toggle confirms off before disabling and on after enabling`() = runTest(dispatcher) {
        val store = FakeSettingsStore()
        val requests = mutableListOf<Pair<AppHapticEffect, Boolean>>()
        val haptics = FakeHapticEffectManager { effect -> requests += effect to store.read().hapticsEnabled }
        val viewModel = createViewModel(store, haptics)

        viewModel.processIntent(SettingsIntent.HapticsChanged(false))
        advanceUntilIdle()
        assertFalse(store.read().hapticsEnabled)
        viewModel.processIntent(SettingsIntent.HapticsChanged(true))
        advanceUntilIdle()
        assertTrue(store.read().hapticsEnabled)
        viewModel.processIntent(SettingsIntent.HapticsChanged(true))
        advanceUntilIdle()

        assertEquals(
            listOf(AppHapticEffect.ToggleOff to true, AppHapticEffect.ToggleOn to true),
            requests,
        )
        assertEquals(listOf("haptics:false", "haptics:true"), store.writes)
    }

    @Test
    fun `saved user is available immediately and external changes are observed`() = runTest(dispatcher) {
        val session = FakeSessionManager(AuthUser("віктор"))
        val viewModel = createViewModel(FakeSettingsStore(), session = session)
        assertEquals("віктор", viewModel.state.value.userName)
        advanceUntilIdle()

        session.saveUser(AuthUser("Anna"))
        advanceUntilIdle()

        assertEquals("Anna", viewModel.state.value.userName)
    }

    @Test
    fun `logout delegates cleanup once and prevents preference writes while waiting`() = runTest(dispatcher) {
        val store = FakeSettingsStore()
        val completion = CompletableDeferred<Unit>()
        val session = FakeSessionManager(onLogout = { completion.await() })
        val haptics = FakeHapticEffectManager()
        val viewModel = createViewModel(store, haptics, session)
        viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(SettingsIntent.LogoutClicked)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isLoggingOut)
        viewModel.processIntent(SettingsIntent.DarkThemeChanged(true))
        viewModel.processIntent(SettingsIntent.LogoutClicked)
        advanceUntilIdle()
        assertTrue(store.writes.isEmpty())

        completion.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, session.logoutCalls)
        assertNull(session.user.value)
        assertTrue(store.writes.isEmpty())
        assertEquals(listOf(AppHapticEffect.Click), haptics.performed)
        assertFalse(viewModel.state.value.isLoggingOut)
    }

    @Test
    fun `failed cleanup keeps the account visible and permits retry`() = runTest(dispatcher) {
        var shouldFail = true
        val session = FakeSessionManager(onLogout = { if (shouldFail) error("Disk unavailable") })
        val viewModel = createViewModel(FakeSettingsStore(), session = session)
        viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(SettingsIntent.LogoutClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.logoutError)
        assertFalse(viewModel.state.value.isLoggingOut)
        assertEquals("Alex", viewModel.state.value.userName)
        assertEquals(AuthUser("Alex"), session.user.value)

        shouldFail = false
        viewModel.processIntent(SettingsIntent.LogoutClicked)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.logoutError)
        assertNull(session.user.value)
        assertEquals(2, session.logoutCalls)
    }

    private fun createViewModel(
        store: AppSettingsStore,
        haptics: HapticEffectManager = FakeHapticEffectManager(),
        session: SessionManager = FakeSessionManager(),
    ) =
        SettingsViewModel(
            settingsStore = store,
            settingsReducer = SettingsReducer(),
            hapticEffectManager = haptics,
            sessionManager = session,
            logger = NoOpLogger,
        ).also(viewModels::add)

    private class FakeHapticEffectManager(
        private val onPerform: (AppHapticEffect) -> Unit = {},
    ) : HapticEffectManager {
        override val effects = emptyFlow<AppHapticEffect>()
        val performed = mutableListOf<AppHapticEffect>()

        override fun performBlocking(effect: AppHapticEffect) {
            onPerform(effect)
            performed += effect
        }

        override suspend fun perform(effect: AppHapticEffect) {
            onPerform(effect)
            performed += effect
        }
    }

    private class FakeSettingsStore(initial: AppSettings = AppSettings()) : AppSettingsStore {
        private val values = MutableStateFlow(initial)
        override val settings = values.asStateFlow()
        val writes = mutableListOf<String>()
        val subscriptionCount: Int get() = values.subscriptionCount.value

        override fun read(): AppSettings = values.value

        override suspend fun setDarkTheme(enabled: Boolean) {
            writes += "darkTheme:$enabled"
            values.update { it.copy(darkTheme = enabled) }
        }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            writes += "haptics:$enabled"
            values.update { it.copy(hapticsEnabled = enabled) }
        }

        override suspend fun clear() {
            values.value = AppSettings()
        }

        fun replace(settings: AppSettings) {
            values.value = settings
        }
    }

    private class FakeSessionManager(
        initial: AuthUser? = AuthUser("Alex"),
        private val onLogout: suspend () -> Unit = {},
    ) : SessionManager {
        private val currentUser = MutableStateFlow(initial)
        override val user = currentUser.asStateFlow()
        var logoutCalls = 0

        override suspend fun saveUser(user: AuthUser) {
            currentUser.value = user
        }

        override suspend fun logout() {
            logoutCalls++
            onLogout()
            currentUser.value = null
        }
    }

    private object NoOpLogger : AppLogger {
        override fun log(tag: String, message: String, t: Throwable?) = Unit
    }
}
