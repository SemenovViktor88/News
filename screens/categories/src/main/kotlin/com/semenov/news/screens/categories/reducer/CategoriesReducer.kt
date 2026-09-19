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
                    error = null,
                )

            is CategoriesPartial.Loading ->
                old.copy(
                    isLoading = partial.keepContent.not(),
                    isSwitchingCategory = partial.keepContent,
                    error = null,
                )

            is CategoriesPartial.Success ->
                old.copy(
                    articles = partial.articles,
                    isLoading = false,
                    isSwitchingCategory = false,
                    hasLoaded = true,
                    error = null,
                )

            is CategoriesPartial.Failure ->
                old.copy(
                    isLoading = false,
                    isSwitchingCategory = false,
                    error = partial.error,
                )
        }
}
