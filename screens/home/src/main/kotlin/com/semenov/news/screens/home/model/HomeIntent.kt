package com.semenov.news.screens.home.model

import com.semenov.news.core.ui.mvi.domain.model.UiIntent

sealed interface HomeIntent : UiIntent {
    data object LoadNews : HomeIntent

    data class SearchQueryChanged(val query: String) : HomeIntent

    data object Retry : HomeIntent
}
