package com.semenov.news.core.ui.navigation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.semenov.news.core.ui.navigation.domain.AppDestination
import com.semenov.news.screens.categories.CategoriesScreen
import com.semenov.news.screens.home.HomeScreen

@Composable
fun NewsNavigationHost(
    navigationManager: NavigationManagerImpl,
    activeDestination: AppDestination,
    modifier: Modifier = Modifier,
) {
    val entryProvider =
        entryProvider {
            entry<NavigationKey.Home> { HomeScreen() }
            entry<NavigationKey.Categories> { CategoriesScreen() }
        }
    val homeEntries =
        rememberDecoratedNavEntries(
            backStack = navigationManager.homeBackStack,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider = entryProvider,
        )
    val categoriesEntries =
        rememberDecoratedNavEntries(
            backStack = navigationManager.categoriesBackStack,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider = entryProvider,
        )
    val activeEntries =
        when (activeDestination) {
            AppDestination.Home -> homeEntries
            AppDestination.Categories -> homeEntries + categoriesEntries
        }

    NavDisplay(
        entries = activeEntries,
        modifier = modifier,
        onBack = navigationManager::goBack,
    )
}
