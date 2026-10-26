package com.semenov.news.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.ui.designsystem.component.NewsArticleCard
import com.semenov.news.core.ui.designsystem.component.NewsEmptyState
import com.semenov.news.core.ui.designsystem.component.NewsErrorState
import com.semenov.news.core.ui.designsystem.component.NewsInlineError
import com.semenov.news.core.ui.designsystem.component.NewsScreenHeader
import com.semenov.news.core.ui.designsystem.theme.NewsSpacing
import com.semenov.news.core.ui.mvi.presentation.BaseScreen
import com.semenov.news.screens.home.components.HomeSkeleton
import com.semenov.news.screens.home.model.HomeIntent
import com.semenov.news.screens.home.model.HomeState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    BaseScreen(
        state = state,
        init = viewModel::initialize,
        skeleton = { HomeSkeleton() },
        offlineContent = {
            NewsErrorState(
                message = stringResource(R.string.home_offline_message),
                retryLabel = stringResource(R.string.home_retry),
                onRetry = { viewModel.processIntent(HomeIntent.Retry) },
            )
        },
        errorContent = {
            NewsErrorState(
                message = state.error.toUserMessage(),
                retryLabel = stringResource(R.string.home_retry),
                onRetry = { viewModel.processIntent(HomeIntent.Retry) },
            )
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            HomeHeader(
                query = state.query,
                isSearching = state.isSearching,
                focusManager = focusManager,
                onQueryChanged = { viewModel.processIntent(HomeIntent.SearchQueryChanged(it)) },
            )
            if (state.error != null) {
                NewsInlineError(
                    message = state.error.toUserMessage(),
                    retryLabel = stringResource(R.string.home_retry),
                    onRetry = { viewModel.processIntent(HomeIntent.Retry) },
                )
            }
            HomeContent(
                modifier = Modifier.weight(1f),
                state = state,
                onLoadMore = { viewModel.processIntent(HomeIntent.LoadMore) },
                onRetryLoadMore = { viewModel.processIntent(HomeIntent.RetryLoadMore) },
            )
        }
    }
}

@Composable
private fun HomeHeader(
    query: String,
    isSearching: Boolean,
    focusManager: FocusManager,
    onQueryChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        NewsScreenHeader(
            title = stringResource(R.string.home_title),
            subtitle = stringResource(R.string.home_subtitle),
        )
        Spacer(modifier = Modifier.height(NewsSpacing.large))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(horizontal = NewsSpacing.large),
            placeholder = { Text(stringResource(R.string.home_search_hint)) },
            leadingIcon = { SearchIcon() },
            shape = RoundedCornerShape(20.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NewsSpacing.large)
                    .height(NewsSpacing.extraSmall),
            contentAlignment = Alignment.Center,
        ) {
            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier,
    state: HomeState,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
) {
    if (state.articles.isEmpty() && state.error == null) {
        NewsEmptyState(
            message = stringResource(R.string.home_empty_message),
            modifier = modifier,
        )
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            layoutInfo.totalItemsCount > 0 &&
                lastVisibleIndex >= layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD - 1
        }.distinctUntilChanged()
            .filter { shouldLoadMore -> shouldLoadMore }
            .collect { onLoadMore() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = NewsSpacing.large,
                top = NewsSpacing.medium,
                end = NewsSpacing.large,
                bottom = NewsSpacing.large,
            ),
        verticalArrangement = Arrangement.spacedBy(NewsSpacing.medium),
    ) {
        itemsIndexed(
            items = state.articles,
            key = { index, article -> "${article.url ?: article.title}-$index" },
        ) { _, article ->
            NewsArticleCard(
                article = article,
                untitledArticle = stringResource(R.string.home_untitled_article),
            )
        }
        if (state.isLoadingMore) {
            item(key = "load-more-progress") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(NewsSpacing.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        state.loadMoreError?.let { error ->
            item(key = "load-more-error") {
                NewsInlineError(
                    message = error.toUserMessage(),
                    retryLabel = stringResource(R.string.home_retry),
                    onRetry = onRetryLoadMore,
                )
            }
        }
    }
}

@Composable
private fun SearchIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(22.dp)) {
        val unit = size.width / 22f
        drawCircle(
            color = color,
            radius = 6.5f * unit,
            center = Offset(9f * unit, 9f * unit),
            style = Stroke(width = 1.8f * unit),
        )
        drawLine(
            color = color,
            start = Offset(14f * unit, 14f * unit),
            end = Offset(19f * unit, 19f * unit),
            strokeWidth = 1.8f * unit,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun NetworkError?.toUserMessage(): String =
    when (this) {
        NetworkError.NoInternet -> stringResource(R.string.home_offline_message)
        NetworkError.Timeout -> stringResource(R.string.home_timeout_message)
        else -> stringResource(R.string.home_error_message)
    }

private const val LOAD_MORE_THRESHOLD = 3
