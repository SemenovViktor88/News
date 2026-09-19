package com.semenov.news.screens.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.semenov.news.core.ui.designsystem.component.NewsArticleSkeleton
import com.semenov.news.core.ui.designsystem.component.ShimmerBlock
import com.semenov.news.core.ui.designsystem.theme.NewsSpacing


@Composable
internal fun HomeSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = NewsSpacing.medium,
                top = NewsSpacing.small,
                end = NewsSpacing.medium,
                bottom = NewsSpacing.large,
            ),
        userScrollEnabled = false,
    ) {
        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = NewsSpacing.large),
            ) {
                ShimmerBlock(modifier = Modifier.height(48.dp).width(190.dp))
                Spacer(modifier = Modifier.height(NewsSpacing.medium))
                ShimmerBlock(modifier = Modifier.fillMaxWidth().height(48.dp))
            }
        }
        items(SKELETON_ITEM_COUNT) {
            NewsArticleSkeleton()
        }
    }
}

private const val SKELETON_ITEM_COUNT = 5

@Preview(showBackground = true)
@Composable
private fun HomeSkeletonPreview() {
    MaterialTheme() {
        Box() {
            HomeSkeleton()
        }
    }
}