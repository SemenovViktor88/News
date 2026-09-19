package com.semenov.news.core.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import coil3.compose.AsyncImage
import com.semenov.news.core.ui.designsystem.theme.NewsSizes
import com.semenov.news.core.ui.designsystem.theme.NewsSpacing

@Composable
fun NewsArticleCard(
    title: String,
    description: String?,
    metadata: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = NewsSizes.articleImageHeight)
                    .padding(vertical = NewsSpacing.small),
        ) {
            ArticleImage(
                imageUrl = imageUrl,
                contentDescription = title,
            )
            Spacer(modifier = Modifier.width(NewsSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NewsSpacing.extraSmall))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!metadata.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NewsSpacing.small))
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    }
}

@Composable
private fun ArticleImage(
    imageUrl: String?,
    contentDescription: String,
) {
    val shape = RoundedCornerShape(6.dp)
    var loadFailed by remember(imageUrl) { mutableStateOf(false) }
    Box(
        modifier =
            Modifier
                .size(
                    width = NewsSizes.articleImageWidth,
                    height = NewsSizes.articleImageHeight,
                ).clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank() && !loadFailed) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onError = { loadFailed = true },
            )
        }
    }
}

@Composable
fun NewsArticleSkeleton(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = NewsSizes.articleImageHeight)
                .padding(vertical = NewsSpacing.small),
    ) {
        ShimmerBlock(
            modifier =
                Modifier.size(
                    width = NewsSizes.articleImageWidth,
                    height = NewsSizes.articleImageHeight,
                ),
            shape = RoundedCornerShape(6.dp),
        )
        Spacer(modifier = Modifier.width(NewsSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBlock(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(NewsSizes.skeletonTitleHeight),
            )
            Spacer(modifier = Modifier.height(NewsSpacing.small))
            ShimmerBlock(
                modifier =
                    Modifier
                        .fillMaxWidth(0.88f)
                        .height(NewsSizes.skeletonTextHeight),
            )
            Spacer(modifier = Modifier.height(NewsSpacing.extraSmall))
            ShimmerBlock(
                modifier =
                    Modifier
                        .fillMaxWidth(0.68f)
                        .height(NewsSizes.skeletonTextHeight),
            )
            Spacer(modifier = Modifier.height(NewsSpacing.medium))
            ShimmerBlock(
                modifier =
                    Modifier
                        .fillMaxWidth(0.42f)
                        .height(NewsSizes.skeletonTextHeight),
            )
        }
    }
}
