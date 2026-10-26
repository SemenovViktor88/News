package com.semenov.news.core.ui.navigation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.semenov.news.core.ui.navigation.domain.AppDestination
import com.semenov.news.core.ui.navigation.domain.AppFlow
import com.semenov.news.screens.auth.AuthScreen
import com.semenov.news.screens.categories.CategoriesScreen
import com.semenov.news.screens.home.HomeScreen
import com.semenov.news.screens.settings.SettingsScreen

@Composable
fun NewsNavigationHost(
    navigationManager: NavigationManagerImpl,
    activeDestination: AppDestination,
    flow: AppFlow,
    modifier: Modifier = Modifier,
) {
    // Inactive flows have no entries: popping their decorators clears screen state and ViewModels.
    val authEntries = AuthFlow(active = flow == AppFlow.Auth)
    val regularEntries = RegularFlow(
        navigationManager = navigationManager,
        activeDestination = activeDestination,
        active = flow == AppFlow.Regular,
    )
    NavDisplay(
        entries = if (flow == AppFlow.Auth) authEntries else regularEntries,
        modifier = modifier,
        onBack = { if (flow == AppFlow.Regular) navigationManager.goBack() },
    )
}

@Composable
private fun AuthFlow(active: Boolean): List<NavEntry<NavigationKey>> =
    rememberDecoratedNavEntries(
        backStack = if (active) listOf(NavigationKey.Auth) else emptyList(),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<NavigationKey.Auth> { AuthScreen() }
        },
    )

@Composable
private fun RegularFlow(
    navigationManager: NavigationManagerImpl,
    activeDestination: AppDestination,
    active: Boolean,
): List<NavEntry<NavigationKey>> {
    val entryProvider = entryProvider {
        entry<NavigationKey.Home> { HomeScreen() }
        entry<NavigationKey.Categories> { CategoriesScreen() }
        entry<NavigationKey.Settings> { SettingsScreen() }
    }
    val homeEntries = rememberDecoratedNavEntries(
        backStack = if (active) navigationManager.homeBackStack else emptyList(),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )
    val categoriesEntries = rememberDecoratedNavEntries(
        backStack = if (active) navigationManager.categoriesBackStack else emptyList(),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )
    val settingsEntries = rememberDecoratedNavEntries(
        backStack = if (active) navigationManager.settingsBackStack else emptyList(),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )
    return when (activeDestination) {
        AppDestination.Home -> homeEntries
        AppDestination.Categories -> homeEntries + categoriesEntries
        AppDestination.Settings -> homeEntries + settingsEntries
    }
}
