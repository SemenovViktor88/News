package com.semenov.news.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.ui.designsystem.component.NewsArticleCard
import com.semenov.news.core.ui.designsystem.theme.NewsSpacing
import com.semenov.news.core.ui.mvi.presentation.BaseScreen
import com.semenov.news.screens.home.components.HomeSkeleton
import com.semenov.news.screens.home.model.HomeIntent
import com.semenov.news.screens.home.model.HomeState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
            HomeErrorState(
                message = stringResource(R.string.home_offline_message),
                onRetry = { viewModel.processIntent(HomeIntent.Retry) },
            )
        },
        errorContent = {
            HomeErrorState(
                message = state.error.toUserMessage(),
                onRetry = { viewModel.processIntent(HomeIntent.Retry) },
            )
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
        ) {
            HomeHeader(
                query = state.query,
                isSearching = state.isSearching,
                focusManager = focusManager,
                onQueryChanged = { viewModel.processIntent(HomeIntent.SearchQueryChanged(it)) },
            )
            if (state.error != null) {
                InlineError(
                    message = state.error.toUserMessage(),
                    onRetry = { viewModel.processIntent(HomeIntent.Retry) },
                )
            }
            HomeContent(
                modifier = Modifier.weight(1f),
                state = state,
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
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = NewsSpacing.medium,
                    top = NewsSpacing.medium,
                    end = NewsSpacing.medium,
                ),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(NewsSpacing.medium))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.home_search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
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
) {
    if (state.articles.isEmpty() && state.error == null) {
        HomeEmptyState()
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = NewsSpacing.medium,
                top = NewsSpacing.small,
                end = NewsSpacing.medium,
                bottom = NewsSpacing.large,
            ),
    ) {
        itemsIndexed(
            items = state.articles,
            key = { index, article -> "${article.url ?: article.title}-$index" },
        ) { _, article ->
            NewsArticleCard(
                title = article.title?.takeIf(String::isNotBlank) ?: stringResource(R.string.home_untitled_article),
                description = article.description,
                metadata = article.metadata(),
                imageUrl = article.imageUrl,
            )
        }
    }
}

@Composable
private fun HomeEmptyState() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(NewsSpacing.large),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_empty_message),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(NewsSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(NewsSpacing.medium))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.home_retry))
        }
    }
}

@Composable
private fun InlineError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = NewsSpacing.medium,
                    vertical = NewsSpacing.small,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.home_retry))
        }
    }
}

@Composable
private fun NetworkError?.toUserMessage(): String =
    when (this) {
        NetworkError.NoInternet -> stringResource(R.string.home_offline_message)
        NetworkError.Timeout -> stringResource(R.string.home_timeout_message)
        else -> stringResource(R.string.home_error_message)
    }

private fun Article.metadata(): String? {
    val parts =
        listOfNotNull(
            source?.takeIf(String::isNotBlank),
            publishedAt.toDisplayDate(),
        )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(METADATA_SEPARATOR)
}

private fun String?.toDisplayDate(): String? =
    this?.let { value ->
        runCatching {
            DATE_FORMATTER.format(Instant.parse(value).atZone(ZoneId.systemDefault()))
        }.getOrNull()
    }

private const val METADATA_SEPARATOR = "  •  "
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
