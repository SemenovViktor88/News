package com.semenov.news.core.ui.interactions.domain.model.dialog

import androidx.annotation.StringRes

data class CheckboxAction(
    @get:StringRes val label: Int,
    val initiallyChecked: Boolean = false,
    val onClick: (Boolean) -> Unit = {},
)
