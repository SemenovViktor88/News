package com.semenov.news.core.ui.interactions.data.manager

import com.semenov.news.core.domain.settings.AppSettingsStore
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter

@Singleton
class HapticEffectManagerImpl @Inject constructor(
    private val settingsStore: AppSettingsStore,
) : HapticEffectManager {
    private val mutableEffects = MutableSharedFlow<AppHapticEffect>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val effects = mutableEffects.asSharedFlow().filter { effect ->
        // Preserve the final confirmation accepted before haptic feedback was disabled.
        effect == AppHapticEffect.ToggleOff || settingsStore.read().hapticsEnabled
    }

    override fun performBlocking(effect: AppHapticEffect) {
        if (settingsStore.read().hapticsEnabled) {
            mutableEffects.tryEmit(effect)
        }
    }

    override suspend fun perform(effect: AppHapticEffect) {
        if (settingsStore.read().hapticsEnabled) {
            mutableEffects.emit(effect)
        }
    }
}
