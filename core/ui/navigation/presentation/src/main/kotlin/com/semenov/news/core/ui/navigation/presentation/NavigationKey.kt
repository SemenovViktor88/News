package com.semenov.news.core.ui.navigation.presentation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

internal sealed interface NavigationKey : NavKey {
    @Serializable
    data object Auth : NavigationKey

    @Serializable
    data object Home : NavigationKey

    @Serializable
    data object Categories : NavigationKey

    @Serializable
    data object Settings : NavigationKey
}
