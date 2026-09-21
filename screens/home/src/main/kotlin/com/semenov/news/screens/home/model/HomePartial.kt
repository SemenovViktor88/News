package com.semenov.news.screens.home.model

import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.ui.mvi.domain.model.UiPartial

sealed interface HomePartial : UiPartial {
    data class QueryChanged(val query: String) : HomePartial

    data class FirstPageLoading(val clearContent: Boolean) : HomePartial

    data class CacheLoaded(val articles: List<Article>) : HomePartial

    data class FirstPageSuccess(
        val articles: List<Article>,
        val totalResults: Int,
    ) : HomePartial

    data class FirstPageFailure(val error: NetworkError) : HomePartial

    data object LoadMoreStarted : HomePartial

    data class LoadMoreSuccess(
        val page: Int,
        val articles: List<Article>,
        val totalResults: Int,
    ) : HomePartial

    data class LoadMoreFailure(val error: NetworkError) : HomePartial
}
