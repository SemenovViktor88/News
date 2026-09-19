package com.semenov.news.screens.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.ui.designsystem.component.NewsArticleCard
import com.semenov.news.core.ui.designsystem.component.NewsEmptyState
import com.semenov.news.core.ui.designsystem.component.NewsErrorState
import com.semenov.news.core.ui.designsystem.component.NewsInlineError
import com.semenov.news.core.ui.designsystem.theme.NewsSpacing
import com.semenov.news.core.ui.mvi.presentation.BaseScreen
import com.semenov.news.screens.categories.components.CategoriesSkeleton
import com.semenov.news.screens.categories.model.CategoriesIntent
import com.semenov.news.screens.categories.model.CategoriesState

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoriesContent(
        state = state,
        onInitialize = viewModel::initialize,
        onIntent = viewModel::processIntent,
    )
}

@Composable
internal fun CategoriesContent(
    state: CategoriesState,
    onInitialize: () -> Unit,
    onIntent: (CategoriesIntent) -> Unit,
) {
    BaseScreen(
        state = state,
        init = onInitialize,
        skeleton = { CategoriesSkeleton() },
        offlineContent = {
            NewsErrorState(
                message = stringResource(R.string.categories_offline_message),
                retryLabel = stringResource(R.string.categories_retry),
                onRetry = { onIntent(CategoriesIntent.Retry) },
            )
        },
        errorContent = {
            NewsErrorState(
                message = state.error.toUserMessage(),
                retryLabel = stringResource(R.string.categories_retry),
                onRetry = { onIntent(CategoriesIntent.Retry) },
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
            CategoriesHeader(
                selectedCategory = state.selectedCategory,
                isSwitchingCategory = state.isSwitchingCategory,
                onCategorySelected = { onIntent(CategoriesIntent.CategorySelected(it)) },
            )
            if (state.error != null) {
                NewsInlineError(
                    message = state.error.toUserMessage(),
                    retryLabel = stringResource(R.string.categories_retry),
                    onRetry = { onIntent(CategoriesIntent.Retry) },
                )
            }
            CategoryArticles(
                modifier = Modifier.weight(1f),
                state = state,
            )
        }
    }
}

@Composable
private fun CategoriesHeader(
    selectedCategory: NewsCategory,
    isSwitchingCategory: Boolean,
    onCategorySelected: (NewsCategory) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = NewsSpacing.medium),
    ) {
        Text(
            text = stringResource(R.string.categories_title),
            modifier = Modifier.padding(horizontal = NewsSpacing.medium),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(NewsSpacing.medium))
        LazyRow(
            contentPadding = PaddingValues(horizontal = NewsSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(NewsSpacing.small),
        ) {
            items(
                items = CATEGORY_ORDER,
                key = NewsCategory::apiValue,
            ) { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.label()) },
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(NewsSpacing.extraSmall),
            contentAlignment = Alignment.Center,
        ) {
            if (isSwitchingCategory) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CategoryArticles(
    modifier: Modifier,
    state: CategoriesState,
) {
    if (state.articles.isEmpty() && state.error == null) {
        NewsEmptyState(
            message = stringResource(R.string.categories_empty_message),
            modifier = modifier,
        )
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
                article = article,
                untitledArticle = stringResource(R.string.categories_untitled_article),
            )
        }
    }
}

@Composable
private fun NewsCategory.label(): String =
    stringResource(
        when (this) {
            NewsCategory.BUSINESS -> R.string.category_business
            NewsCategory.ENTERTAINMENT -> R.string.category_entertainment
            NewsCategory.GENERAL -> R.string.category_general
            NewsCategory.HEALTH -> R.string.category_health
            NewsCategory.SCIENCE -> R.string.category_science
            NewsCategory.SPORTS -> R.string.category_sports
            NewsCategory.TECHNOLOGY -> R.string.category_technology
        },
    )

@Composable
private fun NetworkError?.toUserMessage(): String =
    when (this) {
        NetworkError.NoInternet -> stringResource(R.string.categories_offline_message)
        NetworkError.Timeout -> stringResource(R.string.categories_timeout_message)
        else -> stringResource(R.string.categories_error_message)
    }

private val CATEGORY_ORDER =
    listOf(
        NewsCategory.GENERAL,
        NewsCategory.BUSINESS,
        NewsCategory.TECHNOLOGY,
        NewsCategory.SPORTS,
        NewsCategory.SCIENCE,
        NewsCategory.HEALTH,
        NewsCategory.ENTERTAINMENT,
    )
