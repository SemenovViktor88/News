package com.semenov.news.screens.categories.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.semenov.news.core.ui.designsystem.component.NewsArticleSkeleton
import com.semenov.news.core.ui.designsystem.component.ShimmerBlock
import com.semenov.news.core.ui.designsystem.theme.NewsSpacing

@Composable
internal fun CategoriesSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = NewsSpacing.medium,
                top = NewsSpacing.large,
                end = NewsSpacing.medium,
                bottom = NewsSpacing.large,
            ),
        userScrollEnabled = false,
    ) {
        item(key = "categories-header-skeleton") {
            Column(modifier = Modifier.fillMaxWidth()) {
                ShimmerBlock(
                    modifier =
                        Modifier
                            .width(180.dp)
                            .height(40.dp),
                )
                Spacer(modifier = Modifier.height(NewsSpacing.medium))
                Row {
                    repeat(CATEGORY_SKELETON_COUNT) { index ->
                        ShimmerBlock(
                            modifier =
                                Modifier
                                    .width(if (index == 0) 96.dp else 112.dp)
                                    .height(40.dp),
                        )
                        Spacer(modifier = Modifier.width(NewsSpacing.small))
                    }
                }
                Spacer(modifier = Modifier.height(NewsSpacing.small))
            }
        }
        items(
            count = ARTICLE_SKELETON_COUNT,
            key = { index -> "category-article-skeleton-$index" },
        ) {
            NewsArticleSkeleton()
        }
    }
}

private const val CATEGORY_SKELETON_COUNT = 3
private const val ARTICLE_SKELETON_COUNT = 5
