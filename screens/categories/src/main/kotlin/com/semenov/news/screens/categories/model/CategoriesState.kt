package com.semenov.news.screens.categories.model

import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.ui.mvi.domain.model.ErrorState
import com.semenov.news.core.ui.mvi.domain.model.UiState

data class CategoriesState(
    val selectedCategory: NewsCategory = NewsCategory.GENERAL,
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val isSwitchingCategory: Boolean = false,
    val hasLoaded: Boolean = false,
    val error: NetworkError? = null,
) : UiState {
    override val hasContent: Boolean
        get() = hasLoaded

    override val initialErrorState: ErrorState?
        get() = error?.takeUnless { hasLoaded }?.let(::ErrorState)
}
