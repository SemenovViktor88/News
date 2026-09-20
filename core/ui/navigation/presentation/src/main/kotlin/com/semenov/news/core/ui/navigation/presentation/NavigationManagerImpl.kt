package com.semenov.news.core.ui.navigation.presentation

import androidx.navigation3.runtime.NavBackStack
import com.semenov.news.core.ui.navigation.domain.AppDestination
import com.semenov.news.core.ui.navigation.domain.NavigationManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class NavigationManagerImpl @Inject constructor() : NavigationManager {
    internal val homeBackStack = NavBackStack<NavigationKey>(NavigationKey.Home)
    internal val categoriesBackStack = NavBackStack<NavigationKey>(NavigationKey.Categories)

    private val _activeDestination = MutableStateFlow<AppDestination>(AppDestination.Home)
    override val activeDestination: StateFlow<AppDestination> = _activeDestination.asStateFlow()

    override fun navigate(destination: AppDestination) {
        _activeDestination.value = destination
    }

    override fun goBack() {
        val backStack = backStackFor(activeDestination.value)
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else if (activeDestination.value != AppDestination.Home) {
            _activeDestination.value = AppDestination.Home
        }
    }

    internal fun backStackFor(destination: AppDestination): NavBackStack<NavigationKey> =
        when (destination) {
            AppDestination.Home -> homeBackStack
            AppDestination.Categories -> categoriesBackStack
        }

    internal val activeBackStack: NavBackStack<NavigationKey>
        get() = backStackFor(activeDestination.value)
}
