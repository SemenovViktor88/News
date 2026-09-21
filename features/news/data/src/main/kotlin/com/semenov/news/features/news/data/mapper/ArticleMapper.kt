package com.semenov.news.features.news.data.mapper

import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.features.news.data.model.ArticleDto
import com.semenov.news.features.news.data.model.NewsResponseDto
import com.semenov.news.features.news.data.model.SourceDto
import com.semenov.news.features.news.data.model.SourcesResponseDto

internal fun ArticleDto.toDomain(): Article =
    Article(
        source = source?.name,
        author = author,
        title = title,
        description = description,
        url = url,
        imageUrl = urlToImage,
        publishedAt = publishedAt,
        content = content,
    )

internal fun NewsResponseDto.toDomain(): NewsPage =
    NewsPage(
        articles = articles.map(ArticleDto::toDomain),
        totalResults = totalResults.coerceAtLeast(0),
    )

internal fun SourceDto.toDomain(): NewsSource =
    NewsSource(
        id = id,
        name = name,
        description = description,
        url = url,
        category = NewsCategory.entries.firstOrNull { it.apiValue == category },
        language = language,
        country = country,
    )

internal fun SourcesResponseDto.toDomain(): List<NewsSource> = sources.map(SourceDto::toDomain)
