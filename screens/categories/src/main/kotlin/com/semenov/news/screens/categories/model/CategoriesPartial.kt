package com.semenov.news.screens.categories.model

import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.ui.mvi.domain.model.UiPartial

sealed interface CategoriesPartial : UiPartial {
    data class CategoryChanged(val category: NewsCategory) : CategoriesPartial

    data class Loading(val keepContent: Boolean) : CategoriesPartial

    data class Success(val articles: List<Article>) : CategoriesPartial

    data class Failure(val error: NetworkError) : CategoriesPartial
}
