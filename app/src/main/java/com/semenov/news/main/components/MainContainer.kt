package com.semenov.news.main.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.semenov.news.R
import com.semenov.news.core.ui.mvi.presentation.BaseScreen
import com.semenov.news.core.ui.navigation.domain.AppDestination
import com.semenov.news.core.ui.navigation.domain.AppFlow
import com.semenov.news.core.ui.navigation.presentation.NavigationManagerImpl
import com.semenov.news.core.ui.navigation.presentation.NewsNavigationHost
import com.semenov.news.main.MainViewModel
import com.semenov.news.main.models.MainIntent

@Composable
fun MainContainer(
    viewModel: MainViewModel,
    navigationManager: NavigationManagerImpl,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BaseScreen(
        modifier = Modifier.fillMaxSize(),
        state = state,
        init = viewModel::initialize,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (state.flow == AppFlow.Regular) {
                    MainBottomBar(
                        activeDestination = state.activeDestination,
                        onDestinationSelected = {
                            viewModel.processIntent(MainIntent.DestinationSelected(it))
                        },
                    )
                }
            },
        ) { contentPadding ->
            NewsNavigationHost(
                navigationManager = navigationManager,
                activeDestination = state.activeDestination,
                flow = state.flow,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .consumeWindowInsets(contentPadding),
            )
        }
    }
}

@Composable
private fun MainBottomBar(
    activeDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    val itemColors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        MAIN_DESTINATIONS.forEach { item ->
            NavigationBarItem(
                modifier = Modifier.testTag(item.testTag),
                colors = itemColors,
                selected = activeDestination == item.destination,
                onClick = { onDestinationSelected(item.destination) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}

private data class MainDestinationItem(
    val destination: AppDestination,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String,
)

private val MAIN_DESTINATIONS =
    listOf(
        MainDestinationItem(
            destination = AppDestination.Home,
            labelRes = R.string.navigation_home,
            icon = Icons.Outlined.Home,
            testTag = "navigation_home",
        ),
        MainDestinationItem(
            destination = AppDestination.Categories,
            labelRes = R.string.navigation_categories,
            icon = Icons.AutoMirrored.Outlined.List,
            testTag = "navigation_categories",
        ),
        MainDestinationItem(
            destination = AppDestination.Settings,
            labelRes = R.string.navigation_settings,
            icon = Icons.Outlined.Settings,
            testTag = "navigation_settings",
        ),
    )
