package com.semenov.news.screens.categories.reducer

import com.semenov.news.core.ui.mvi.domain.Reducer
import com.semenov.news.screens.categories.model.CategoriesPartial
import com.semenov.news.screens.categories.model.CategoriesState
import javax.inject.Inject

class CategoriesReducer @Inject constructor() : Reducer<CategoriesState, CategoriesPartial> {
    override suspend fun reduce(
        partial: CategoriesPartial,
        old: CategoriesState,
    ): CategoriesState =
        when (partial) {
            is CategoriesPartial.CategoryChanged ->
                old.copy(
                    selectedCategory = partial.category,
                    articles = emptyList(),
                    isLoadingMore = false,
                    loadMoreError = null,
                    hasMore = true,
                    currentPage = 1,
                    hasLoaded = false,
                    error = null,
                )

            is CategoriesPartial.FirstPageLoading ->
                if (partial.clearContent) {
                    old.copy(
                        articles = emptyList(),
                        isLoading = true,
                        isSwitchingCategory = false,
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
                        isSwitchingCategory = old.hasLoaded,
                        isLoadingMore = false,
                        loadMoreError = null,
                        hasMore = true,
                        currentPage = 1,
                        error = null,
                    )
                }

            is CategoriesPartial.CacheLoaded ->
                old.copy(
                    isLoading = false,
                    isSwitchingCategory = true,
                    articles = partial.articles,
                    hasLoaded = true,
                    error = null,
                )

            is CategoriesPartial.FirstPageSuccess -> {
                val articles = partial.articles.distinctByArticleIdentity()
                old.copy(
                    articles = articles,
                    isLoading = false,
                    isSwitchingCategory = false,
                    isLoadingMore = false,
                    loadMoreError = null,
                    hasMore = hasMore(partial.articles, FIRST_PAGE, partial.totalResults),
                    currentPage = 1,
                    hasLoaded = true,
                    error = null,
                )
            }

            is CategoriesPartial.FirstPageFailure ->
                old.copy(
                    isLoading = false,
                    isSwitchingCategory = false,
                    isLoadingMore = false,
                    error = partial.error.takeUnless { old.hasLoaded },
                )

            CategoriesPartial.LoadMoreStarted ->
                old.copy(
                    isLoadingMore = true,
                    loadMoreError = null,
                )

            is CategoriesPartial.LoadMoreSuccess -> {
                val articles = (old.articles + partial.articles).distinctByArticleIdentity()
                old.copy(
                    articles = articles,
                    isLoadingMore = false,
                    loadMoreError = null,
                    hasMore = hasMore(partial.articles, partial.page, partial.totalResults),
                    currentPage = partial.page,
                )
            }

            is CategoriesPartial.LoadMoreFailure ->
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
