package com.semenov.news.screens.categories

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.features.news.domain.repository.NewsRepository
import com.semenov.news.screens.categories.model.CategoriesEffect
import com.semenov.news.screens.categories.model.CategoriesIntent
import com.semenov.news.screens.categories.model.CategoriesPartial
import com.semenov.news.screens.categories.model.CategoriesState
import com.semenov.news.screens.categories.reducer.CategoriesReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val categoriesReducer: CategoriesReducer,
    logger: AppLogger,
) : MviViewModel<CategoriesState, CategoriesPartial, CategoriesIntent, CategoriesEffect>(
        initialState = CategoriesState(),
        logger = logger,
    ) {
    private var requestJob: Job? = null
    private var loadMoreJob: Job? = null

    override suspend fun processInit() {
        startFirstPage(currentState.selectedCategory, clearContent = true)
    }

    override suspend fun handleIntent(intent: CategoriesIntent) {
        when (intent) {
            CategoriesIntent.LoadCategory,
            CategoriesIntent.Retry,
                -> startFirstPage(currentState.selectedCategory, clearContent = false)

            is CategoriesIntent.CategorySelected -> {
                if (intent.category == currentState.selectedCategory) return
                sendPartial(CategoriesPartial.CategoryChanged(intent.category))
                startFirstPage(intent.category, clearContent = true)
            }

            CategoriesIntent.LoadMore -> startLoadMore(isRetry = false)
            CategoriesIntent.RetryLoadMore -> startLoadMore(isRetry = true)
        }
    }

    override suspend fun reduceWithPartial(
        partial: CategoriesPartial,
        old: CategoriesState,
    ): CategoriesState = categoriesReducer.reduce(partial, old)

    private fun startFirstPage(
        category: NewsCategory,
        clearContent: Boolean,
    ) {
        val request = category.toRequest()
        requestJob?.cancel()
        loadMoreJob?.cancel()
        requestJob =
            launch {
                loadFirstPage(request, clearContent)
            }
    }

    private suspend fun loadFirstPage(
        request: NewsRequest,
        clearContent: Boolean,
    ) {
        sendPartial(CategoriesPartial.FirstPageLoading(clearContent))
        val cachedArticles = repository.cachedArticles(request)
        if (cachedArticles.isNotEmpty()) {
            sendPartial(CategoriesPartial.CacheLoaded(cachedArticles))
        }

        when (val result = repository.loadPage(request, FIRST_PAGE)) {
            is DataResult.Success -> {
                sendPartial(
                    CategoriesPartial.FirstPageSuccess(
                        articles = result.data.articles,
                        totalResults = result.data.totalResults,
                    ),
                )
            }

            is DataResult.Failure -> sendPartial(CategoriesPartial.FirstPageFailure(result.error))
        }
    }

    private fun startLoadMore(isRetry: Boolean) {
        val state = currentState
        if (
            loadMoreJob?.isActive == true ||
            state.isLoadingMore ||
            state.hasMore.not() ||
            state.isLoading ||
            (!isRetry && state.loadMoreError != null)
        ) {
            return
        }

        val activeRequestJob = requestJob
        loadMoreJob =
            launch {
                activeRequestJob?.join()
                val latestState = currentState
                if (
                    latestState.hasMore.not() ||
                    latestState.isLoading ||
                    (!isRetry && latestState.loadMoreError != null)
                ) {
                    return@launch
                }

                val page = latestState.currentPage + 1
                val request = latestState.selectedCategory.toRequest()
                sendPartial(CategoriesPartial.LoadMoreStarted)
                when (val result = repository.loadPage(request, page)) {
                    is DataResult.Success ->
                        sendPartial(
                            CategoriesPartial.LoadMoreSuccess(
                                page = page,
                                articles = result.data.articles,
                                totalResults = result.data.totalResults,
                            ),
                        )

                    is DataResult.Failure -> sendPartial(CategoriesPartial.LoadMoreFailure(result.error))
                }
            }
    }

    private fun NewsCategory.toRequest() = NewsRequest.Category(apiValue)

    private companion object {
        const val FIRST_PAGE = 1
    }
}
