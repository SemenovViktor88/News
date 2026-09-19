package com.semenov.news.screens.home.model

import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.ui.mvi.domain.model.UiPartial

sealed interface HomePartial : UiPartial {
    data class QueryChanged(val query: String) : HomePartial

    data class Loading(val keepContent: Boolean) : HomePartial

    data class Success(val articles: List<Article>) : HomePartial

    data class Failure(val error: NetworkError) : HomePartial
}