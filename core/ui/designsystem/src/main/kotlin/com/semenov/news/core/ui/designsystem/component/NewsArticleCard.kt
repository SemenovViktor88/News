package com.semenov.news.core.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import coil3.compose.AsyncImage
import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.ui.designsystem.theme.NewsSizes
import com.semenov.news.core.ui.designsystem.theme.NewsSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun NewsArticleCard(
    article: Article,
    untitledArticle: String,
    modifier: Modifier = Modifier,
) {
    NewsArticleCard(
        title = article.title?.takeIf(String::isNotBlank) ?: untitledArticle,
        description = article.description,
        metadata = article.metadata(),
        imageUrl = article.imageUrl,
        modifier = modifier,
    )
}

@Composable
fun NewsArticleCard(
    title: String,
    description: String?,
    metadata: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = NewsSizes.articleImageHeight)
                    .padding(18.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!metadata.isNullOrBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(NewsSpacing.small))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NewsSpacing.small))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(NewsSpacing.medium))
            ArticleImage(imageUrl = imageUrl)
        }
    }
}

@Composable
private fun ArticleImage(
    imageUrl: String?,
) {
    val shape = RoundedCornerShape(16.dp)
    var loadFailed by remember(imageUrl) { mutableStateOf(false) }
    Box(
        modifier =
            Modifier
                .size(
                    width = 88.dp,
                    height = 112.dp,
                ).clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank() && !loadFailed) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onError = { loadFailed = true },
            )
        } else {
            // A quiet placeholder keeps the card balanced for missing or failed images.
            Column(
                modifier = Modifier.padding(NewsSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(3) { index ->
                    Box(
                        modifier =
                            Modifier.fillMaxWidth(if (index == 2) .65f else 1f).height(3.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}

@Composable
fun NewsArticleSkeleton(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBlock(
                    modifier = Modifier.fillMaxWidth(0.55f).height(NewsSizes.skeletonTextHeight),
                )
                Spacer(modifier = Modifier.height(NewsSpacing.small))
                ShimmerBlock(
                    modifier = Modifier.fillMaxWidth().height(NewsSizes.skeletonTitleHeight),
                )
                Spacer(modifier = Modifier.height(NewsSpacing.extraSmall))
                ShimmerBlock(
                    modifier = Modifier.fillMaxWidth(0.88f).height(NewsSizes.skeletonTitleHeight),
                )
                Spacer(modifier = Modifier.height(NewsSpacing.medium))
                ShimmerBlock(
                    modifier = Modifier.fillMaxWidth().height(NewsSizes.skeletonTextHeight),
                )
                Spacer(modifier = Modifier.height(NewsSpacing.extraSmall))
                ShimmerBlock(
                    modifier = Modifier.fillMaxWidth(0.68f).height(NewsSizes.skeletonTextHeight),
                )
            }
            Spacer(modifier = Modifier.width(NewsSpacing.medium))
            ShimmerBlock(
                modifier = Modifier.size(width = 88.dp, height = 112.dp),
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
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
