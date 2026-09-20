package com.semenov.news.core.ui.navigation.domain

sealed interface AppDestination {
    data object Home : AppDestination

    data object Categories : AppDestination
}
