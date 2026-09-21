package com.semenov.news.screens.home

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.features.news.domain.repository.NewsRepository
import com.semenov.news.screens.home.model.HomeEffect
import com.semenov.news.screens.home.model.HomeIntent
import com.semenov.news.screens.home.model.HomePartial
import com.semenov.news.screens.home.model.HomeState
import com.semenov.news.screens.home.reducer.HomeReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val homeReducer: HomeReducer,
    logger: AppLogger,
) : MviViewModel<HomeState, HomePartial, HomeIntent, HomeEffect>(
        initialState = HomeState(),
        logger = logger,
    ) {
    private var requestJob: Job? = null
    private var loadMoreJob: Job? = null

    override suspend fun processInit() {
        startFirstPage(NewsRequest.Feed, clearContent = true)
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.LoadNews,
            HomeIntent.Retry,
            -> startFirstPage(currentRequest(), clearContent = false)

            is HomeIntent.SearchQueryChanged -> onSearchQueryChanged(intent.query)
            HomeIntent.LoadMore -> startLoadMore(isRetry = false)
            HomeIntent.RetryLoadMore -> startLoadMore(isRetry = true)
        }
    }

    override suspend fun reduceWithPartial(
        partial: HomePartial,
        old: HomeState,
    ): HomeState = homeReducer.reduce(partial, old)

    private suspend fun onSearchQueryChanged(query: String) {
        if (query == currentState.query) return
        requestJob?.cancel()
        loadMoreJob?.cancel()
        sendPartial(HomePartial.QueryChanged(query))
        requestJob =
            launch {
                delay(SEARCH_DEBOUNCE_MS.milliseconds)
                loadFirstPage(requestFor(query), clearContent = true)
            }
    }

    private fun startFirstPage(
        request: NewsRequest,
        clearContent: Boolean,
    ) {
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
        sendPartial(HomePartial.FirstPageLoading(clearContent))
        val cachedArticles = repository.cachedArticles(request)
        if (cachedArticles.isNotEmpty()) {
            sendPartial(HomePartial.CacheLoaded(cachedArticles))
        }

        when (val result = repository.loadPage(request, FIRST_PAGE)) {
            is DataResult.Success -> {
                sendPartial(
                    HomePartial.FirstPageSuccess(
                        articles = result.data.articles,
                        totalResults = result.data.totalResults,
                    ),
                )
            }

            is DataResult.Failure -> sendPartial(HomePartial.FirstPageFailure(result.error))
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
                val request = currentRequest()
                sendPartial(HomePartial.LoadMoreStarted)
                when (val result = repository.loadPage(request, page)) {
                    is DataResult.Success ->
                        sendPartial(
                            HomePartial.LoadMoreSuccess(
                                page = page,
                                articles = result.data.articles,
                                totalResults = result.data.totalResults,
                            ),
                        )

                    is DataResult.Failure -> sendPartial(HomePartial.LoadMoreFailure(result.error))
                }
            }
    }

    private fun currentRequest(): NewsRequest = requestFor(currentState.query)

    private fun requestFor(query: String): NewsRequest =
        query.trim().takeIf(String::isNotEmpty)?.let(NewsRequest::Search) ?: NewsRequest.Feed

    private companion object {
        const val FIRST_PAGE = 1
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
