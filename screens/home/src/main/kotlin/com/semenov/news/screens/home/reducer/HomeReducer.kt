package com.semenov.news.screens.home.reducer

import com.semenov.news.core.ui.mvi.domain.Reducer
import com.semenov.news.screens.home.model.HomePartial
import com.semenov.news.screens.home.model.HomeState
import jakarta.inject.Inject

class HomeReducer @Inject constructor() : Reducer<HomeState, HomePartial> {
    override suspend fun reduce(
        partial: HomePartial,
        old: HomeState
    ): HomeState = when (partial) {
        is HomePartial.QueryChanged -> old.copy(query = partial.query, error = null)
        is HomePartial.Loading ->
            old.copy(
                isLoading = partial.keepContent.not(),
                isSearching = partial.keepContent,
                error = null,
            )

        is HomePartial.Success ->
            old.copy(
                articles = partial.articles,
                isLoading = false,
                isSearching = false,
                hasLoaded = true,
                error = null,
            )

        is HomePartial.Failure ->
            old.copy(
                isLoading = false,
                isSearching = false,
                error = partial.error,
            )
    }
}