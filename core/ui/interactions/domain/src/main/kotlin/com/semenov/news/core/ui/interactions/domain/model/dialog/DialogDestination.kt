package com.semenov.news.core.ui.interactions.domain.model.dialog

sealed interface DialogDestination {
    val cancelable: Boolean
    val secondaryAction: Action?
    val primaryAction: Action?
    val checkboxAction: CheckboxAction?

    data class Logout(
        override val cancelable: Boolean = true,
        override val secondaryAction: Action? = null,
        override val primaryAction: Action? = null,
        override val checkboxAction: CheckboxAction? = null,
    ) : DialogDestination
}
