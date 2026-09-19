package com.semenov.news.core.ui.mvi.domain.model

import com.semenov.news.core.domain.model.NetworkError

data class ErrorState(
    val error: NetworkError,
)
