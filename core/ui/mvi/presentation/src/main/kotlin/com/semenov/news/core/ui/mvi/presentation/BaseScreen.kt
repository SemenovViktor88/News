package com.semenov.news.core.ui.mvi.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.ui.mvi.domain.model.UiState

private const val ANIMATION_DURATION_MS = 300

/**
 * Root composable that handles loading / error / offline / content states.
 *
 * @param state         Current [UiState].
 * @param init          Called to (re)load data — passed to default offline/error screens.
 * @param skeleton      Shown while [UiState.hasContent] == false and no error.
 * @param offlineContent Shown when [UiState.initialErrorState] contains a no-network error.
 * @param errorContent  Shown on any other error when there's no content yet.
 * @param content       Shown when [UiState.hasContent] == true.
 */
@Composable
fun <State : UiState> BaseScreen(
    modifier: Modifier = Modifier,
    state: State,
    init: () -> Unit,
    offlineContent: @Composable () -> Unit = {
        DefaultOfflineScreen(onRetryClick = init)
    },
    errorContent: @Composable () -> Unit = {
        DefaultErrorScreen(onRetryClick = init)
    },
    skeleton: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val error = state.initialErrorState?.error
    val fadeSpec = remember { tween<Float>(ANIMATION_DURATION_MS) }

    val isNoNetwork = error is NetworkError.NoInternet
    val showOffline = isNoNetwork && state.hasContent.not()
    val showError = showOffline.not() && error != null && state.hasContent.not()
    val showSkeleton = showOffline.not() && showError.not() && state.hasContent.not()
    val showContent = showOffline.not() && showError.not() && showSkeleton.not()

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showOffline,
            enter = fadeIn(fadeSpec),
            exit = fadeOut(fadeSpec),
        ) { offlineContent() }

        AnimatedVisibility(
            visible = showError,
            enter = fadeIn(fadeSpec),
            exit = fadeOut(fadeSpec),
        ) { errorContent() }

        AnimatedVisibility(
            visible = showSkeleton,
            enter = fadeIn(fadeSpec),
            exit = fadeOut(fadeSpec),
        ) { skeleton() }

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(fadeSpec),
            exit = fadeOut(fadeSpec),
        ) { content() }
    }
}

// ── Default placeholders ──────────────────────────────────────────────────────

@Composable
fun DefaultOfflineScreen(onRetryClick: () -> Unit = {}) {
    var enabled by remember { mutableStateOf(true) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            enabled = enabled,
            onClick = {
                enabled = false
                onRetryClick()
            },
        ) {
            Text("No internet. Retry")
        }
    }
}

@Composable
fun DefaultErrorScreen(onRetryClick: () -> Unit = {}) {
    var enabled by remember { mutableStateOf(true) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            enabled = enabled,
            onClick = {
                enabled = false
                onRetryClick()
            },
        ) {
            Text("Something went wrong. Retry")
        }
    }
}
