package com.semenov.news.screens.categories

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.domain.model.TopHeadlinesRequest
import com.semenov.news.core.ui.mvi.presentation.MviViewModel
import com.semenov.news.features.news.domain.repository.NewsRepository
import com.semenov.news.screens.categories.model.CategoriesEffect
import com.semenov.news.screens.categories.model.CategoriesIntent
import com.semenov.news.screens.categories.model.CategoriesPartial
import com.semenov.news.screens.categories.model.CategoriesState
import com.semenov.news.screens.categories.reducer.CategoriesReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val categoriesReducer: CategoriesReducer,
    logger: AppLogger,
) : MviViewModel<CategoriesState, CategoriesPartial, CategoriesIntent, CategoriesEffect>(
        initialState = CategoriesState(),
        logger = logger,
    ) {
    private var requestGeneration = 0L

    override suspend fun processInit() {
        loadCategory(currentState.selectedCategory)
    }

    override suspend fun handleIntent(intent: CategoriesIntent) {
        when (intent) {
            CategoriesIntent.LoadCategory -> loadCategory(currentState.selectedCategory)
            CategoriesIntent.Retry -> loadCategory(currentState.selectedCategory)
            is CategoriesIntent.CategorySelected -> {
                if (intent.category == currentState.selectedCategory) return
                sendPartial(CategoriesPartial.CategoryChanged(intent.category))
                loadCategory(intent.category)
            }
        }
    }

    override suspend fun reduceWithPartial(
        partial: CategoriesPartial,
        old: CategoriesState,
    ): CategoriesState = categoriesReducer.reduce(partial, old)

    private suspend fun loadCategory(category: NewsCategory) {
        val generation = ++requestGeneration
        sendPartial(CategoriesPartial.Loading(keepContent = currentState.hasLoaded))
        when (
            val result =
                repository.getTopHeadlines(
                    TopHeadlinesRequest(
                        country = DEFAULT_HEADLINES_COUNTRY,
                        category = category,
                    ),
                )
        ) {
            is DataResult.Success -> {
                if (generation == requestGeneration) {
                    sendPartial(CategoriesPartial.Success(result.data.articles))
                }
            }

            is DataResult.Failure -> {
                if (generation == requestGeneration) {
                    sendPartial(CategoriesPartial.Failure(result.error))
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_HEADLINES_COUNTRY = "us"
    }
}
