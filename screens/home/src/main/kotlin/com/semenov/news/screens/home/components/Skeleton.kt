package com.semenov.news.screens.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.semenov.news.core.ui.designsystem.component.NewsArticleSkeleton
import com.semenov.news.core.ui.designsystem.component.ShimmerBlock
import com.semenov.news.core.ui.designsystem.theme.NewsSpacing
import com.semenov.news.core.ui.designsystem.theme.NewsTheme

@Composable
internal fun HomeSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding =
            PaddingValues(
                start = NewsSpacing.large,
                top = 32.dp,
                end = NewsSpacing.large,
                bottom = NewsSpacing.large,
            ),
        verticalArrangement = Arrangement.spacedBy(NewsSpacing.medium),
        userScrollEnabled = false,
    ) {
        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = NewsSpacing.extraSmall),
            ) {
                ShimmerBlock(modifier = Modifier.height(42.dp).width(220.dp))
                Spacer(modifier = Modifier.height(7.dp))
                ShimmerBlock(modifier = Modifier.height(20.dp).fillMaxWidth(.8f))
                Spacer(modifier = Modifier.height(NewsSpacing.large))
                ShimmerBlock(modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(20.dp))
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
    NewsTheme {
        Box {
            HomeSkeleton()
        }
    }
}
