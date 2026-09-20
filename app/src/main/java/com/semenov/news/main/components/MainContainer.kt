package com.semenov.news.main.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.semenov.news.R
import com.semenov.news.core.ui.mvi.presentation.BaseScreen
import com.semenov.news.core.ui.navigation.domain.AppDestination
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
            bottomBar = {
                MainBottomBar(
                    activeDestination = state.activeDestination,
                    onDestinationSelected = {
                        viewModel.processIntent(MainIntent.DestinationSelected(it))
                    },
                )
            },
        ) { contentPadding ->
            NewsNavigationHost(
                navigationManager = navigationManager,
                activeDestination = state.activeDestination,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
            )
        }
    }
}

@Composable
private fun MainBottomBar(
    activeDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    NavigationBar {
        MAIN_DESTINATIONS.forEach { item ->
            NavigationBarItem(
                selected = activeDestination == item.destination,
                onClick = { onDestinationSelected(item.destination) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.labelRes),
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
)

private val MAIN_DESTINATIONS =
    listOf(
        MainDestinationItem(
            destination = AppDestination.Home,
            labelRes = R.string.navigation_home,
            icon = Icons.Outlined.Home,
        ),
        MainDestinationItem(
            destination = AppDestination.Categories,
            labelRes = R.string.navigation_categories,
            icon = Icons.AutoMirrored.Outlined.List,
        ),
    )
