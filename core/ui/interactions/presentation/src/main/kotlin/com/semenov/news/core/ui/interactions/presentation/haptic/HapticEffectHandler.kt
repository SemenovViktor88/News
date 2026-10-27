package com.semenov.news.core.ui.interactions.presentation.haptic

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.semenov.news.core.ui.interactions.domain.model.AppHapticEffect
import com.semenov.news.core.ui.interactions.presentation.extensions.CollectEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun HapticEffectHandler(
    hapticEffects: Flow<AppHapticEffect>,
) {
    val hapticFeedback = LocalHapticFeedback.current

    hapticEffects.CollectEffect(additionalKey = hapticFeedback) { appHapticEffect ->
        hapticFeedback.performHapticFeedback(appHapticEffect.asHapticFeedbackType)
    }
}

val AppHapticEffect.asHapticFeedbackType: HapticFeedbackType
    get() = when (this) {
        AppHapticEffect.Click -> HapticFeedbackType.VirtualKey
        AppHapticEffect.LongPress -> HapticFeedbackType.LongPress
        AppHapticEffect.Confirm -> HapticFeedbackType.Confirm
        AppHapticEffect.Reject -> HapticFeedbackType.Reject
        AppHapticEffect.ToggleOn -> HapticFeedbackType.ToggleOn
        AppHapticEffect.ToggleOff -> HapticFeedbackType.ToggleOff
        AppHapticEffect.SegmentTick -> HapticFeedbackType.SegmentTick
        AppHapticEffect.SegmentFrequentTick -> HapticFeedbackType.SegmentFrequentTick
        AppHapticEffect.GestureThresholdActivate -> HapticFeedbackType.GestureThresholdActivate
        AppHapticEffect.GestureEnd -> HapticFeedbackType.GestureEnd
    }
