package com.semenov.news.core.ui.interactions.data

import com.semenov.news.core.domain.settings.AppSettings
import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.ui.interactions.data.manager.HapticEffectManagerImpl
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HapticEffectManagerImplTest {
    @Test
    fun `blocking effects reach an active collector`() = runTest {
        val manager = HapticEffectManagerImpl(FakeAppSettingsStore())
        val received = mutableListOf<AppHapticEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.effects.collect(received::add)
        }

        manager.performBlocking(AppHapticEffect.Click)
        manager.performBlocking(AppHapticEffect.ToggleOn)

        assertEquals(listOf(AppHapticEffect.Click, AppHapticEffect.ToggleOn), received)
    }

    @Test
    fun `suspending effects reach an active collector`() = runTest {
        val manager = HapticEffectManagerImpl(FakeAppSettingsStore())
        val received = mutableListOf<AppHapticEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.effects.collect(received::add)
        }

        manager.perform(AppHapticEffect.Confirm)
        manager.perform(AppHapticEffect.GestureEnd)

        assertEquals(listOf(AppHapticEffect.Confirm, AppHapticEffect.GestureEnd), received)
    }

    @Test
    fun `blocking bursts retain the most recent effects while collector is busy`() = runTest {
        val manager = HapticEffectManagerImpl(FakeAppSettingsStore())
        val received = mutableListOf<AppHapticEffect>()
        val releaseCollector = CompletableDeferred<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.effects.collect { effect ->
                received += effect
                releaseCollector.await()
            }
        }
        manager.performBlocking(AppHapticEffect.LongPress)
        val burst = List(80) { AppHapticEffect.entries[it % AppHapticEffect.entries.size] }

        burst.forEach(manager::performBlocking)
        assertEquals(listOf(AppHapticEffect.LongPress), received)
        releaseCollector.complete(Unit)
        runCurrent()

        assertEquals(listOf(AppHapticEffect.LongPress) + burst.takeLast(64), received)
    }

    @Test
    fun `effects emitted without collectors are not replayed later`() = runTest {
        val manager = HapticEffectManagerImpl(FakeAppSettingsStore())
        manager.performBlocking(AppHapticEffect.Click)
        manager.perform(AppHapticEffect.Confirm)
        val received = mutableListOf<AppHapticEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.effects.collect(received::add)
        }

        assertTrue(received.isEmpty())

        manager.performBlocking(AppHapticEffect.ToggleOff)

        assertEquals(listOf(AppHapticEffect.ToggleOff), received)
    }

    @Test
    fun `both APIs suppress disabled effects and resume immediately when enabled`() = runTest {
        val settingsStore = FakeAppSettingsStore()
        val manager = HapticEffectManagerImpl(settingsStore)
        val received = mutableListOf<AppHapticEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.effects.collect(received::add)
        }
        settingsStore.setHapticsEnabled(false)

        manager.performBlocking(AppHapticEffect.Click)
        manager.perform(AppHapticEffect.Confirm)
        manager.performBlocking(AppHapticEffect.ToggleOff)
        manager.perform(AppHapticEffect.ToggleOff)

        assertTrue(received.isEmpty())

        settingsStore.setHapticsEnabled(true)
        manager.performBlocking(AppHapticEffect.ToggleOn)
        manager.perform(AppHapticEffect.GestureEnd)

        assertEquals(listOf(AppHapticEffect.ToggleOn, AppHapticEffect.GestureEnd), received)
    }

    @Test
    fun `queued effects are suppressed when disabled before delivery`() = runTest {
        val settingsStore = FakeAppSettingsStore()
        val manager = HapticEffectManagerImpl(settingsStore)
        val received = mutableListOf<AppHapticEffect>()
        val releaseCollector = CompletableDeferred<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.effects.collect { effect ->
                received += effect
                releaseCollector.await()
            }
        }
        manager.performBlocking(AppHapticEffect.LongPress)
        manager.performBlocking(AppHapticEffect.Click)
        manager.perform(AppHapticEffect.Confirm)

        settingsStore.setHapticsEnabled(false)
        releaseCollector.complete(Unit)
        runCurrent()

        assertEquals(listOf(AppHapticEffect.LongPress), received)

        settingsStore.setHapticsEnabled(true)
        manager.performBlocking(AppHapticEffect.ToggleOn)

        assertEquals(listOf(AppHapticEffect.LongPress, AppHapticEffect.ToggleOn), received)
    }

    @Test
    fun `accepted toggle off survives disabling before delivery`() = runTest {
        val settingsStore = FakeAppSettingsStore()
        val manager = HapticEffectManagerImpl(settingsStore)
        val received = mutableListOf<AppHapticEffect>()
        val releaseCollector = CompletableDeferred<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.effects.collect { effect ->
                received += effect
                releaseCollector.await()
            }
        }
        manager.performBlocking(AppHapticEffect.LongPress)
        manager.perform(AppHapticEffect.ToggleOff)

        settingsStore.setHapticsEnabled(false)
        manager.performBlocking(AppHapticEffect.ToggleOff)
        manager.perform(AppHapticEffect.ToggleOff)
        releaseCollector.complete(Unit)
        runCurrent()

        assertEquals(listOf(AppHapticEffect.LongPress, AppHapticEffect.ToggleOff), received)
    }

    private class FakeAppSettingsStore : AppSettingsStore {
        private val mutableSettings = MutableStateFlow(AppSettings())
        override val settings = mutableSettings.asStateFlow()

        override fun read(): AppSettings = mutableSettings.value

        override suspend fun setDarkTheme(enabled: Boolean) {
            mutableSettings.value = read().copy(darkTheme = enabled)
        }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            mutableSettings.value = read().copy(hapticsEnabled = enabled)
        }

        override suspend fun clear() {
            mutableSettings.value = AppSettings()
        }
    }
}
