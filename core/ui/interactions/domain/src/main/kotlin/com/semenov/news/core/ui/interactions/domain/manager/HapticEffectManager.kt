package com.semenov.news.core.ui.interactions.domain.manager

import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import kotlinx.coroutines.flow.Flow

interface HapticEffectManager {
    val effects: Flow<AppHapticEffect>

    fun performBlocking(effect: AppHapticEffect)

    suspend fun perform(effect: AppHapticEffect)
}
