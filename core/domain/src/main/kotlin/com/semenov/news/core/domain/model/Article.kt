package com.semenov.news.core.domain.model

data class Article(
    val source: String?,
    val author: String?,
    val title: String?,
    val description: String?,
    val url: String?,
    val imageUrl: String?,
    val publishedAt: String?,
    val content: String?,
)

data class NewsPage(
    val articles: List<Article>,
    val totalResults: Int,
)

data class NewsSource(
    val id: String?,
    val name: String?,
    val description: String?,
    val url: String?,
    val category: NewsCategory?,
    val language: String?,
    val country: String?,
)
