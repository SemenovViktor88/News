package com.semenov.news.screens.categories.model

import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.ui.mvi.domain.model.UiPartial

sealed interface CategoriesPartial : UiPartial {
    data class CategoryChanged(val category: NewsCategory) : CategoriesPartial

    data class FirstPageLoading(val clearContent: Boolean) : CategoriesPartial

    data class CacheLoaded(val articles: List<Article>) : CategoriesPartial

    data class FirstPageSuccess(
        val articles: List<Article>,
        val totalResults: Int,
    ) : CategoriesPartial

    data class FirstPageFailure(val error: NetworkError) : CategoriesPartial

    data object LoadMoreStarted : CategoriesPartial

    data class LoadMoreSuccess(
        val page: Int,
        val articles: List<Article>,
        val totalResults: Int,
    ) : CategoriesPartial

    data class LoadMoreFailure(val error: NetworkError) : CategoriesPartial
}
