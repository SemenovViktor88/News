package com.semenov.news.screens.home.reducer

import com.semenov.news.core.ui.mvi.domain.Reducer
import com.semenov.news.screens.home.model.HomePartial
import com.semenov.news.screens.home.model.HomeState
import jakarta.inject.Inject

class HomeReducer @Inject constructor() : Reducer<HomeState, HomePartial> {
    override suspend fun reduce(
        partial: HomePartial,
        old: HomeState
    ): HomeState =
        when (partial) {
            is HomePartial.QueryChanged ->
                old.copy(
                    query = partial.query,
                    isLoadingMore = false,
                    loadMoreError = null,
                    error = null,
                )

            is HomePartial.FirstPageLoading ->
                if (partial.clearContent) {
                    old.copy(
                        articles = emptyList(),
                        isLoading = true,
                        isSearching = false,
                        isLoadingMore = false,
                        loadMoreError = null,
                        hasMore = true,
                        currentPage = 1,
                        hasLoaded = false,
                        error = null,
                    )
                } else {
                    old.copy(
                        isLoading = old.hasLoaded.not(),
                        isSearching = old.hasLoaded,
                        isLoadingMore = false,
                        loadMoreError = null,
                        hasMore = true,
                        currentPage = 1,
                        error = null,
                    )
                }

            is HomePartial.CacheLoaded ->
                old.copy(
                    articles = partial.articles,
                    isLoading = false,
                    isSearching = true,
                    hasLoaded = true,
                    error = null,
                )

            is HomePartial.FirstPageSuccess -> {
                val articles = partial.articles.distinctByArticleIdentity()
                old.copy(
                    articles = articles,
                    isLoading = false,
                    isSearching = false,
                    isLoadingMore = false,
                    loadMoreError = null,
                    hasMore = hasMore(partial.articles, FIRST_PAGE, partial.totalResults),
                    currentPage = 1,
                    hasLoaded = true,
                    error = null,
                )
            }

            is HomePartial.FirstPageFailure ->
                old.copy(
                    isLoading = false,
                    isSearching = false,
                    isLoadingMore = false,
                    error = partial.error.takeUnless { old.hasLoaded },
                )

            HomePartial.LoadMoreStarted ->
                old.copy(
                    isLoadingMore = true,
                    loadMoreError = null,
                )

            is HomePartial.LoadMoreSuccess -> {
                val articles = (old.articles + partial.articles).distinctByArticleIdentity()
                old.copy(
                    articles = articles,
                    isLoadingMore = false,
                    loadMoreError = null,
                    hasMore = hasMore(partial.articles, partial.page, partial.totalResults),
                    currentPage = partial.page,
                )
            }

            is HomePartial.LoadMoreFailure ->
                old.copy(
                    isLoadingMore = false,
                    loadMoreError = partial.error,
                )
        }

    private fun List<com.semenov.news.core.domain.model.Article>.distinctByArticleIdentity() =
        distinctBy { article -> article.url ?: article.title }

    private fun hasMore(
        returnedArticles: List<com.semenov.news.core.domain.model.Article>,
        page: Int,
        totalResults: Int,
    ): Boolean =
        returnedArticles.isNotEmpty() &&
            page * PAGE_SIZE < totalResults.coerceAtMost(FREE_TIER_LIMIT)

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 20
        const val FREE_TIER_LIMIT = 100
    }
}
