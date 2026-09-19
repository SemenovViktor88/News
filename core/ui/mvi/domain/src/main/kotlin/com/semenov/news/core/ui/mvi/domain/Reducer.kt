package com.semenov.news.core.ui.mvi.domain

import com.semenov.news.core.ui.mvi.domain.model.UiPartial
import com.semenov.news.core.ui.mvi.domain.model.UiState

interface Reducer<State : UiState, Partial : UiPartial> {
    suspend fun reduce(
        partial: Partial,
        old: State,
    ): State
}
