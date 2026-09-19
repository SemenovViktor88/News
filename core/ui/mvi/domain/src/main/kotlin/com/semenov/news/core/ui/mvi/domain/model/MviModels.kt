package com.semenov.news.core.ui.mvi.domain.model

/**
 * The user actions.
 */
interface UiIntent

/**
 * The partial state
 */
interface UiPartial

/**
 * Current state of views.
 */
interface UiState {
    val hasContent: Boolean
    val initialErrorState: ErrorState?
}

/**
 * The side effects which we want to show only once.
 */
interface UiEffect
