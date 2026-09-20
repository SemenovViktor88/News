package com.semenov.news.screens.home

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.SearchNewsRequest
import com.semenov.news.core.domain.model.TopHeadlinesRequest
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.features.news.domain.repository.NewsRepository
import com.semenov.news.screens.home.model.HomeEffect
import com.semenov.news.screens.home.model.HomeIntent
import com.semenov.news.screens.home.model.HomePartial
import com.semenov.news.screens.home.model.HomeState
import com.semenov.news.screens.home.reducer.HomeReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val homeReducer: HomeReducer,
    logger: AppLogger,
) : MviViewModel<HomeState, HomePartial, HomeIntent, HomeEffect>(
        initialState = HomeState(),
        logger = logger,
    ) {

    override suspend fun processInit() {
        loadHeadlines()
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.LoadNews -> loadHeadlines()
            HomeIntent.Retry -> loadForQuery(currentState.query)
            is HomeIntent.SearchQueryChanged -> {
                val query = intent.query
                sendPartial(HomePartial.QueryChanged(query))
                loadForQuery(query)
            }
        }
    }

    override suspend fun reduceWithPartial(
        partial: HomePartial,
        old: HomeState,
    ): HomeState = homeReducer.reduce(partial, old)

    private suspend fun loadForQuery(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            loadHeadlines()
        } else {
            executeRequest {
                repository.searchNews(SearchNewsRequest(query = normalizedQuery))
            }
        }
    }

    private suspend fun loadHeadlines() {
        executeRequest {
            repository.getTopHeadlines(TopHeadlinesRequest(country = DEFAULT_HEADLINES_COUNTRY))
        }
    }

    private suspend fun executeRequest(
        request: suspend () -> DataResult<NewsPage>,
    ) {
        sendPartial(HomePartial.Loading(keepContent = currentState.hasLoaded))
        when (val result = request()) {
            is DataResult.Success -> sendPartial(HomePartial.Success(result.data.articles))

            is DataResult.Failure -> sendPartial(HomePartial.Failure(result.error))
        }
    }

    private companion object {
        const val DEFAULT_HEADLINES_COUNTRY = "us"
    }
}
