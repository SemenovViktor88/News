package com.semenov.news.core.ui.interactions.domain.model.dialog

import androidx.annotation.StringRes

data class Action(
    @get:StringRes val label: Int,
    val onClick: () -> Unit = {},
)
