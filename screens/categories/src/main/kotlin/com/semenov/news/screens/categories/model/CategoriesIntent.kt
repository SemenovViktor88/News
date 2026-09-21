package com.semenov.news.screens.categories.model

import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.ui.mvi.domain.model.UiIntent

sealed interface CategoriesIntent : UiIntent {
    data object LoadCategory : CategoriesIntent

    data class CategorySelected(val category: NewsCategory) : CategoriesIntent

    data object Retry : CategoriesIntent

    data object LoadMore : CategoriesIntent

    data object RetryLoadMore : CategoriesIntent
}
