package com.semenov.news.core.db.mapper

import com.semenov.news.core.db.entity.ArticleEntity
import com.semenov.news.core.domain.model.Article

fun Article.toEntityOrNull(
    contextKey: String,
    page: Int,
    position: Int,
    cachedAt: Long,
): ArticleEntity? {
    val stableUrl = url?.takeIf(String::isNotBlank) ?: return null
    return ArticleEntity(
        contextKey = contextKey,
        url = stableUrl,
        title = title,
        description = description,
        imageUrl = imageUrl,
        publishedAt = publishedAt,
        sourceName = source,
        author = author,
        content = content,
        page = page,
        position = position,
        cachedAt = cachedAt,
    )
}

fun ArticleEntity.toDomain(): Article =
    Article(
        source = sourceName,
        author = author,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        publishedAt = publishedAt,
        content = content,
    )
