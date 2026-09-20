package com.semenov.news.core.ui.navigation.domain

import kotlinx.coroutines.flow.StateFlow

interface NavigationManager {
    val activeDestination: StateFlow<AppDestination>

    fun navigate(destination: AppDestination)

    fun goBack()
}
