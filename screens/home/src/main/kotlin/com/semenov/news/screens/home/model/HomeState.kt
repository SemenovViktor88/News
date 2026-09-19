package com.semenov.news.screens.home.model

import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.ui.mvi.domain.model.ErrorState
import com.semenov.news.core.ui.mvi.domain.model.UiState

data class HomeState(
    val query: String = "",
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val hasLoaded: Boolean = false,
    val error: NetworkError? = null,
) : UiState {
    override val hasContent: Boolean
        get() = hasLoaded

    override val initialErrorState: ErrorState?
        get() = error?.takeUnless { hasLoaded }?.let(::ErrorState)
}